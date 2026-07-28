package com.shrawan.resumebuilder.service;

import com.shrawan.resumebuilder.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.shrawan.resumebuilder.util.AppConstants.PREMIUM;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplatesService {
    private final AuthService authService;

    public Map<String,Object> getTemplates(Object principal){
        //1. get the current profile
        AuthResponse authResponse = authService.getProfile(principal);
        //2. get the available templates based on the subscription
        List<String> availableTemplates;

        boolean isPremium = PREMIUM.equalsIgnoreCase(authResponse.getSubscriptionPlan());

        if(isPremium ){
            availableTemplates = List.of("01","02","03");
        }else{
            availableTemplates = List.of("01");
        }
        //3. add the data into the map

        Map<String,Object> restrictions = new HashMap<>();
        restrictions.put("availableTemplates",availableTemplates);
        restrictions.put("allTemplates",List.of("01","02","03"));
        restrictions.put("subscriptionPlan",authResponse.getSubscriptionPlan());
        restrictions.put("isPremium",isPremium);

        //4. return the result

        return restrictions;

    }


}
