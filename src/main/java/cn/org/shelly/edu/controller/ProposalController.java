package cn.org.shelly.edu.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name= "提案赛管理")
@RequiredArgsConstructor
@RequestMapping("/proposal")
public class ProposalController {

}
