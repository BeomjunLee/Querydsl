package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
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

    /**
     * fetchResults()로 자동으로 쿼리를 2방 날리기
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                //.orderBy()이런게 들어가도 total 카운트 쿼리에선 생략됨
                .offset(pageable.getOffset())   //몇 번부터 시작
                .limit(pageable.getPageSize())  //한번에 몇개 까지 조회할지
                .fetchResults();//querydsl 이 content 쿼리, count 쿼리 두번 날림  (fetch 는 content 만)

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();    //total count

        return new PageImpl<>(content, pageable, total);    //Page 의 구현체에 (content, pageable, total) 순
    }

    /**
     * fetch()를 써서 쿼리를 분리해서 날리기
     * -select 쿼리가 복잡한데 비에 total count 쿼리는 간단하게 만들 수 있을 때 사용 (total count 최적화)
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                //.orderBy()이런게 들어가도 total 카운트 쿼리에선 생략됨
                .offset(pageable.getOffset())   //몇 번부터 시작
                .limit(pageable.getPageSize())  //한번에 몇개 까지 조회할지
                .fetch();//querydsl 이 content 쿼리, count 쿼리 두번 날림  (fetch 는 content 만)

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );


//        return new PageImpl<>(content, pageable, total);    //Page 의 구현체에 (content, pageable, total) 순

        //첫 페이지나 마지막 일 때는 자동으로 count 쿼리가 안나간다 -> pageableExecutionUtils
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
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
