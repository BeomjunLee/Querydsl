package study.querydsl.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * 이름은 꼭 MemberRepository + Impl  (규칙)
 */
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    /**
     * 4가지 검색조건 동적 쿼리 
     */
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                    .select(new QMemberTeamDto(
                            member.id.as("memberId"),
                            member.username,
                            member.age,
                            team.id.as("teamId"),
                            team.name.as("teamName")))
                    .from(member)
                    .leftJoin(member.team, team)
                    .where(
                            usernameEq(condition.getUsername()),
                            teamNameEq(condition.getTeamName()),
                            ageGoe(condition.getAgeGoe()),
                            ageLoe(condition.getAgeLoe())
                    )
                    .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        if(hasText(username)) return member.username.eq(username);
        return null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        if(hasText(teamName)) return team.name.eq(teamName);
        return null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        if(ageGoe != null) return member.age.goe(ageGoe);
        return null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        if(ageLoe != null) return member.age.loe(ageLoe);
        return null;
    }
}
