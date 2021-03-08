package study.querydsl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;     //Integer 를 쓰는이유 : 값이 null 일 수도 있어서
    private Integer ageLoe;
}
