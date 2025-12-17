package com.stackroute.userauthenticationservice.service;
import com.stackroute.userauthenticationservice.model.UserCredential;
import java.util.Map;
public interface TokenGeneratorService {
    Map<String,String> generateToken(UserCredential credential);
}
