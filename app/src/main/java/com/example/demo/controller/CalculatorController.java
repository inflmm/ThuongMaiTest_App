package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/calculator")
public class CalculatorController {
    @GetMapping("/add")
    public ResponseEntity<String> add (@RequestParam String a, @RequestParam String b){
        try{
            BigDecimal  paramA = new BigDecimal(a);
            BigDecimal  paramB = new BigDecimal(b);
            BigDecimal result = paramA.add(paramB);
            return ResponseEntity.ok(result.toString());
        } catch (NumberFormatException e){
            return ResponseEntity.badRequest().body("Lỗi: Tham số truyền vào không hợp lệ");
        }
    }

    @GetMapping("/subtract")
    public ResponseEntity<String> subtract(@RequestParam String a, @RequestParam String b){
        try{
            BigDecimal paramA = new BigDecimal(a);
            BigDecimal paramB = new BigDecimal(b);
            BigDecimal result = paramA.subtract(paramB);
            return ResponseEntity.ok(result.toString());
        } catch (NumberFormatException e){
            return ResponseEntity.badRequest().body("Lỗi: Tham số truyền vào không hợp lệ");
        }
    }

    @GetMapping("/multiply")
    public ResponseEntity<String> multiply (@RequestParam String a, @RequestParam String b){
        try{
            BigDecimal paramA = new BigDecimal(a);
            BigDecimal paramB = new BigDecimal(b);
            BigDecimal result = paramA.multiply(paramB);
            return ResponseEntity.ok(result.toString());
        } catch (NumberFormatException e){
            return ResponseEntity.badRequest().body("Lỗi: Tham số truyền vào không hợp lệ");
        }
    }

    @GetMapping("/divide")
    public ResponseEntity<String> divide (@RequestParam String a, @RequestParam String b){
        try{
            BigDecimal paramA = new BigDecimal(a);
            BigDecimal paramB = new BigDecimal(b);
            if(paramB.compareTo(BigDecimal.ZERO) == 0){
                return ResponseEntity.badRequest().body("Lỗi: Không thể chia cho 0");
            }
            BigDecimal result = paramA.divide(paramB, 4, RoundingMode.CEILING);
            return ResponseEntity.ok(result.toString());
        } catch (NumberFormatException e){
            return ResponseEntity.badRequest().body("Lỗi: Tham số truyền vào không hợp lệ");
        }
    }
}
