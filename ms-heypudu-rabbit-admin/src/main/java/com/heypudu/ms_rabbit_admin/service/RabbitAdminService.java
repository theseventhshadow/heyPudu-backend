package com.heypudu.ms_rabbit_admin.service;

import com.heypudu.ms_rabbit_admin.dto.BindingRequest;
import com.heypudu.ms_rabbit_admin.dto.ExchangeRequest;
import com.heypudu.ms_rabbit_admin.dto.QueueRequest;

public interface RabbitAdminService {

    void createExchange(String name, ExchangeRequest request);

    void deleteExchange(String name);

    void createQueue(String name, QueueRequest request);

    void deleteQueue(String name);

    void createBinding(BindingRequest request);

    void deleteBinding(BindingRequest request);
}
