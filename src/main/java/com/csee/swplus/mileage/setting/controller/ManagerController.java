package com.csee.swplus.mileage.setting.controller;

import com.csee.swplus.mileage.setting.dto.ManagerResponse;
import com.csee.swplus.mileage.setting.entity.Manager;
import com.csee.swplus.mileage.setting.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mileage")
@RequiredArgsConstructor
@Slf4j
public class ManagerController {
    private final ManagerService managerService;

    @GetMapping("/apply")
    public ManagerResponse getMileageSetting() {
        Manager manager = managerService.getRegisterDate();

        if (manager == null) {
            log.error("Manager 정보가 없습니다.");
            return null;
        }

        return new ManagerResponse(manager.getRegStart(), manager.getRegEnd());
    }
}
