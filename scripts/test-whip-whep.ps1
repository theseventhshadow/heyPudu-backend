<#
.SYNOPSIS
    Prueba local de WHIP (publicar) y WHEP (negociar escucha) contra MediaMTX,
    usando un JWT real de Cognito.

.DESCRIPTION
    - Crea una sala en ms-live (o reutiliza una con -RoomId).
    - Valida contra ms-live que el JWT autoriza 'publish' y 'read'.
    - Hace un handshake WHEP con Authorization: Bearer <JWT>. Sin publicador
      se espera un 404 (autenticacion aceptada) en lugar de 201.
    - Con -PublishWithFfmpeg intenta publicar audio con ffmpeg (Docker) y
      comprueba que MediaMTX marca el path como ready.

    El token se pide de forma segura o se toma de HEYPUDU_TEST_JWT.
    No se imprime en pantalla.

.EXAMPLE
    $env:HEYPUDU_TEST_JWT = "<id-token>"
    .\scripts\test-whip-whep.ps1

.EXAMPLE
    .\scripts\test-whip-whep.ps1 -DurationSeconds 30 -KeepRoom
#>
param(
    [string]$Token = $env:HEYPUDU_TEST_JWT,
    [string]$RoomId,
    [int]$DurationSeconds = 20,
    [string]$MsLiveBaseUrl = 'http://localhost:8086',
    [string]$MediaMtxBaseUrl = 'http://localhost:8889',
    [string]$MediaMtxApiUrl = 'http://localhost:9997',
    [string]$MediamtxServiceToken = 'dev-mediamtx-token',
    [switch]$PublishWithFfmpeg,
    [switch]$KeepRoom
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Message) {
    Write-Host "`n=== $Message ===" -ForegroundColor Cyan
}

function Normalize-Jwt([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return $Value }

    # Extrae el JWT aunque el texto traiga cabeceras, cURL, comillas o saltos.
    $match = [regex]::Match($Value, 'eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+')
    if ($match.Success) { return $match.Value }

    $token = $Value.Trim()
    $token = $token -replace '^[Bb]earer\s+', ''
    $token = $token -replace '\s', ''
    return $token.Trim('"', "'")
}

function Get-JwtPayload([string]$Jwt) {
    $clean = Normalize-Jwt $Jwt
    $parts = $clean.Split('.')
    if ($parts.Length -lt 3) {
        throw 'El token no tiene formato JWT (se esperan 3 partes separadas por puntos).'
    }
    $payload = $parts[1].Replace('-', '+').Replace('_', '/')
    switch ($payload.Length % 4) {
        2 { $payload += '==' }
        3 { $payload += '=' }
    }
    try {
        $json = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
    } catch {
        throw 'El payload del token no es Base64 valido. Copia solo el token, sin comillas, espacios ni saltos de linea.'
    }
    return ($json | ConvertFrom-Json)
}

function New-SdpOffer {
    $ufrag = -join ((1..8) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
    $pwd = -join ((1..24) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
    $fingerprint = (1..32 | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) }) -join ':'
    return @"
v=0
o=- 0 0 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE 0
a=msid-semantic: WMS *
m=audio 9 UDP/TLS/RTP/SAVPF 111
c=IN IP4 0.0.0.0
a=rtcp:9 IN IP4 0.0.0.0
a=ice-ufrag:$ufrag
a=ice-pwd:$pwd
a=ice-options:trickle
a=fingerprint:sha-256 $fingerprint
a=setup:actpass
a=mid:0
a=recvonly
a=rtcp-mux
a=rtpmap:111 opus/48000/2
a=fmtp:111 minptime=10;useinbandfec=1
"@
}

function Test-MediamtxAuth([string]$Action) {
    $body = @{
        user     = $sub
        password = $Token
        token    = $Token
        action   = $Action
        path     = $RoomId
        protocol = 'webrtc'
    } | ConvertTo-Json
    try {
        Invoke-WebRequest -Method Post `
            -Uri "$MsLiveBaseUrl/api/v1/internal/mediamtx/auth?serviceToken=$MediamtxServiceToken" `
            -ContentType 'application/json' -Body $body -UseBasicParsing | Out-Null
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        throw "Autorizacion '$Action' fallo con HTTP $status. Detalle: $($_.Exception.Message)"
    }
}

function Get-DockerExe {
    $local = Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'
    if (Test-Path $local) { return $local }
    return 'docker'
}

# ------------------------------------------------------------
# Token
# ------------------------------------------------------------
if ([string]::IsNullOrWhiteSpace($Token)) {
    $secure = Read-Host -AsSecureString 'Pega el ID token de Cognito (no se mostrara)'
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $Token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

$Token = Normalize-Jwt $Token
if ([string]::IsNullOrWhiteSpace($Token)) {
    throw 'No se proporciono token.'
}

$payload = Get-JwtPayload $Token
$sub = $payload.sub
if ([string]::IsNullOrWhiteSpace($sub)) {
    throw 'El token no contiene el claim sub.'
}
if ($payload.exp) {
    $expiresAt = [DateTimeOffset]::FromUnixTimeSeconds([long]$payload.exp)
    if ($expiresAt -lt [DateTimeOffset]::UtcNow) {
        throw "El token expiro a las $($expiresAt.ToString('u')). Genera uno nuevo."
    }
    Write-Host "Token valido para sub=$sub (expira $($expiresAt.ToString('u')))."
} else {
    Write-Host "Token para sub=$sub."
}

$authHeaders = @{ Authorization = "Bearer $Token" }
$createdRoom = $false
$publisherContainer = 'heypudu-whip-test'
$whepLocation = $null

# ------------------------------------------------------------
# Servicios
# ------------------------------------------------------------
Write-Step 'Comprobando servicios'
try {
    Invoke-RestMethod -Uri "$MsLiveBaseUrl/actuator/health" | Out-Null
    Write-Host "ms-live OK ($MsLiveBaseUrl)"
} catch {
    throw "ms-live no responde en $MsLiveBaseUrl. Levantalo antes de ejecutar la prueba."
}
try {
    Invoke-RestMethod -Uri "$MediaMtxApiUrl/v3/config/global/get" | Out-Null
    Write-Host "MediaMTX API OK ($MediaMtxApiUrl)"
} catch {
    throw "MediaMTX no responde en $MediaMtxApiUrl. Ejecuta docker compose -f docker-compose.mediamtx.yml up -d."
}

$dockerExe = Get-DockerExe

try {
    # --------------------------------------------------------
    # Sala
    # --------------------------------------------------------
    Write-Step 'Sala'
    if ([string]::IsNullOrWhiteSpace($RoomId)) {
        $body = @{ title = 'Prueba WHIP/WHEP'; description = 'Script local' } | ConvertTo-Json
        $room = Invoke-RestMethod -Method Post -Uri "$MsLiveBaseUrl/api/v1/live/rooms" `
            -Headers $authHeaders -ContentType 'application/json' -Body $body
        $RoomId = $room.id
        $createdRoom = $true
        Write-Host "Sala creada: $RoomId"
    } else {
        Write-Host "Usando sala existente: $RoomId"
    }

    Write-Step 'Autorizacion de MediaMTX'
    Test-MediamtxAuth 'publish'
    Write-Host 'publish autorizado.'
    Test-MediamtxAuth 'read'
    Write-Host 'read autorizado.'

    # --------------------------------------------------------
    # Publicador WHIP (opcional con ffmpeg en Docker)
    # --------------------------------------------------------
    $ready = $false
    if ($PublishWithFfmpeg) {
        Write-Step 'Publicando audio con WHIP (ffmpeg)'
        & $dockerExe rm -f $publisherContainer 2>$null | Out-Null
        $ffmpegArgs = @(
            'run', '-d', '--name', $publisherContainer,
            '--add-host=host.docker.internal:host-gateway',
            '--entrypoint', 'ffmpeg',
            'linuxserver/ffmpeg:latest',
            '-re',
            '-f', 'lavfi',
            '-i', 'sine=frequency=440:sample_rate=48000',
            '-t', "$DurationSeconds",
            '-c:a', 'libopus', '-b:a', '64k',
            '-f', 'whip',
            '-authorization', $Token,
            "http://host.docker.internal:8889/$RoomId/whip"
        )
        & $dockerExe @ffmpegArgs | Out-Null

        for ($i = 0; $i -lt 24; $i++) {
            Start-Sleep -Milliseconds 500
            try {
                $path = Invoke-RestMethod -Uri "$MediaMtxApiUrl/v3/paths/get/$RoomId"
                if ($path.ready) { $ready = $true; break }
            } catch {
                # El path aun no existe.
            }
        }

        if (-not $ready) {
            Write-Host 'MediaMTX no reporto el path como ready. Log del publicador:' -ForegroundColor Yellow
            & $dockerExe logs --tail 30 $publisherContainer 2>&1 | Write-Host
            throw 'WHIP no logro publicar. Revisa el log anterior.'
        }
        Write-Host "WHIP OK: path $RoomId ready."
    } else {
        Write-Host 'Publicador ffmpeg omitido. Usa -PublishWithFfmpeg para intentar WHIP.'
    }

    # --------------------------------------------------------
    # Handshake WHEP
    # --------------------------------------------------------
    Write-Step 'Negociando escucha con WHEP'
    $sdp = New-SdpOffer
    $response = $null
    try {
        $response = Invoke-WebRequest -Method Post -Uri "$MediaMtxBaseUrl/$RoomId/whep" `
            -Headers $authHeaders -ContentType 'application/sdp' -Body $sdp -UseBasicParsing
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        $detail = ''
        try {
            $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
            $detail = $reader.ReadToEnd()
        } catch {
            # Sin cuerpo de respuesta.
        }
        if ($status -eq 401 -or $status -eq 403) {
            throw "WHEP rechazo la autenticacion (HTTP $status). Detalle: $detail"
        }
        Write-Host "WHEP respondio HTTP $status (autenticacion aceptada; sin publicador se espera 404)."
    }

    if ($response) {
        $whepLocation = @($response.Headers.Location)[0]
        Write-Host "WHEP OK: HTTP $($response.StatusCode). Location=$whepLocation"
        if ($whepLocation) {
            if ($whepLocation -notmatch '^https?://') {
                $whepLocation = "$MediaMtxBaseUrl$whepLocation"
            }
            Invoke-WebRequest -Method Delete -Uri $whepLocation -Headers $authHeaders -UseBasicParsing | Out-Null
            Write-Host 'Sesion WHEP cerrada.'
        }
    }

    Write-Step 'Resultado'
    if ($ready) {
        Write-Host 'WHIP publico y WHEP autentico correctamente con el JWT.' -ForegroundColor Green
    } else {
        Write-Host 'Autenticacion publish/read OK. WHEP alcanzo MediaMTX sin 401 (no habia stream porque no se publico).' -ForegroundColor Green
    }
} finally {
    Write-Step 'Limpieza'
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    & $dockerExe stop $publisherContainer 2>&1 | Out-Null
    & $dockerExe rm -f $publisherContainer 2>&1 | Out-Null
    $ErrorActionPreference = $previousErrorAction

    if ($createdRoom -and -not $KeepRoom) {
        try {
            Invoke-WebRequest -Method Delete -Uri "$MsLiveBaseUrl/api/v1/live/rooms/$RoomId" `
                -Headers $authHeaders -UseBasicParsing | Out-Null
            Write-Host "Sala $RoomId cerrada."
        } catch {
            Write-Host "No se pudo cerrar la sala $RoomId`: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } elseif ($createdRoom) {
        Write-Host "Sala $RoomId conservada (-KeepRoom)."
    }
}
