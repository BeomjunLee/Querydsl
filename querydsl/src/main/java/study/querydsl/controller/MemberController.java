package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public ResponseEntity searchMemberV1(MemberSearchCondition condition) {
        return ResponseEntity.ok(memberRepository.search(condition));
    }

    @GetMapping("/v2/members")
    public ResponseEntity searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return ResponseEntity.ok(memberRepository.searchPageSimple(condition, pageable));
    }

    @GetMapping("/v3/members")
    public ResponseEntity searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return ResponseEntity.ok(memberRepository.searchPageComplex(condition, pageable));
    }
}
