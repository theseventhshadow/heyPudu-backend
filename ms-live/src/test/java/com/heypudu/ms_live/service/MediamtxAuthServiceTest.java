package com.heypudu.ms_live.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MediamtxAuthServiceTest {

    private static final UUID SUB = UUID.fromString("04d8f468-0051-7090-5bfe-3138b1bf593b");

    @Test
    void extraeElUsuarioDelQueryEscapadoPorMediaMtx() {
        // MediaMTX publica MTX_QUERY con url.QueryEscape: user=<sub> -> user%3D<sub>
        assertEquals(SUB, MediamtxAuthService.extractUser(
                "user%3D04d8f468-0051-7090-5bfe-3138b1bf593b"));
    }

    @Test
    void extraeElUsuarioDelQuerySinEscapar() {
        assertEquals(SUB, MediamtxAuthService.extractUser(
                "user=04d8f468-0051-7090-5bfe-3138b1bf593b"));
    }

    @Test
    void extraeElUsuarioCuandoHayVariosParametros() {
        assertEquals(SUB, MediamtxAuthService.extractUser(
                "key1%3Dval1%26user%3D04d8f468-0051-7090-5bfe-3138b1bf593b"));
    }

    @Test
    void extraeElUsuarioConInterrogacionInicial() {
        assertEquals(SUB, MediamtxAuthService.extractUser(
                "?user=04d8f468-0051-7090-5bfe-3138b1bf593b"));
    }

    @Test
    void devuelveNullSinQuery() {
        assertNull(MediamtxAuthService.extractUser(null));
        assertNull(MediamtxAuthService.extractUser(""));
        assertNull(MediamtxAuthService.extractUser("   "));
    }

    @Test
    void devuelveNullSiNoHayParametroUser() {
        assertNull(MediamtxAuthService.extractUser("otro=1"));
    }

    @Test
    void devuelveNullSiElUsuarioNoEsUnUuid() {
        assertNull(MediamtxAuthService.extractUser("user%3Dno-es-uuid"));
    }
}
