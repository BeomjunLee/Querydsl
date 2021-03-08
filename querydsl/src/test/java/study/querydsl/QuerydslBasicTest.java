package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
@Commit
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception{
      //member1 찾기
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void startQuerydsl() throws Exception{
        //QMember m = new QMember("m");
        //QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void search() {
        //username = member1 and age = 10 찾기
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        
        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void searchAndParam() {
        //username = member1 and age = 10 찾기
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void resultFetch() {
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        //단 건
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();

        //처음 한 건 조회
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();  // limit(1).fetchOne();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal();     //totalCount
        results.getLimit();     //limit
        results.getOffset();    //offset
        List<Member> content = results.getResults();    //getResults 로 List 가져옴

        //count 쿼리
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() throws Exception{
        //데이터 넣기
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertEquals(member5.getUsername(), "member5");
        assertEquals(member6.getUsername(), "member6");
        assertEquals(memberNull.getUsername(), null);
    }

    /**
     * 페이징 테스트
     */
    @Test
    public void paging1() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터시작
                .limit(2)  //2개
                .fetch();

        assertEquals(result.size(), 2); 

    }

    /**
     * 전체 페이징 테스트
     */
    @Test
    public void paging2() throws Exception{
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터시작
                .limit(2)  //2개
                .fetchResults();

        assertEquals(queryResults.getTotal(), 4);
        assertEquals(queryResults.getLimit(), 2);
        assertEquals(queryResults.getOffset(), 1);
        assertEquals(queryResults.getResults().size(), 2);
    }

    /**
     * 집합 함수
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();   //Tuple 로 꺼내짐

        Tuple tuple = result.get(0);
        
        //이런식으로 Tuple 사용
        assertEquals(tuple.get(member.count()), 4);
        assertEquals(tuple.get(member.age.sum()), 100);
        assertEquals(tuple.get(member.age.avg()), 25);
        assertEquals(tuple.get(member.age.max()), 40);
        assertEquals(tuple.get(member.age.min()), 10);

    }

    /**
     * group by
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
    
        //팀이 2개라서 2개가 나옴
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertEquals(teamA.get(team.name), "teamA");
        assertEquals(teamA.get(member.age.avg()), 15);

        assertEquals(teamB.get(team.name), "teamB");
        assertEquals(teamB.get(member.age.avg()), 35);
    }

    /**
     * join
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인 ( from 절에 여러 엔티티를 선택해서 세타조인 가능하지만 외부조인 불가능) -> on 사용시 가능
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * 페치 조인 미적용
     */
    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear(); //페치조인 보기위해 영속성 컨텍스트 초기화

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //member 의 team 이 로딩된 엔티티인지 아닌지 확인
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * 페치 조인 적용
     */
    @Test
    public void fetchJoin() {
        em.flush();
        em.clear(); //페치조인 보기위해 영속성 컨텍스트 초기화

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()    //member 조회할 때 연관된 team 까지 한번에 끌어옴
                .where(member.username.eq("member1"))
                .fetchOne();

        //member 의 team 이 로딩된 엔티티인지 아닌지 확인
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception{

        //서브쿼리 사용할 때는 where select 절의 명칭은 달라야한다
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() throws Exception{

        //서브쿼리 사용할 때는 where select 절의 명칭은 달라야한다
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10이상인 회원 조회 (in절 활용 예시)
     */
    @Test
    public void subQueryIn() throws Exception{

        //서브쿼리 사용할 때는 where select 절의 명칭은 달라야한다
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select 절 서브쿼리
     */
    @Test
    public void selectSubQuery() {
        //서브쿼리 사용할 때는 where select 절의 명칭은 달라야한다
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 나이를 한글로 표시
     */
    @Test
    public void basicCase() throws Exception{
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 0~20살이면 "0~20살"
     * 21~30살이면 "21~30살" 로 표현
     */
    @Test
    public void complexCase() throws Exception{
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수 출력
     */
    @Test
    public void constant() throws Exception{
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기 (member1의 유저 이름과 나이 더하기)
     */
    @Test
    public void concat() throws Exception{
        //{username}_{age}
        //더하려면 타입이 같아야됨
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 튜플
     */
    @Test
    public void tupleProjection() throws Exception{
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * DTO 로 가져오기 (JPQL)
     */
    @Test
    public void findDtoByJQPL() {
        String qlString = "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                "from Member m";
        List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
                .getResultList();
        for (MemberDto dto : result) {
            System.out.println("MemberDto = " + dto);
        }
    }

    /**
     * DTO 로 가져오기 (querydsl setter)
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto dto : result) {
            System.out.println("MemberDto = " + dto);
        }
    }

    /**
     * DTO 로 가져오기 (querydsl field)
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto dto : result) {
            System.out.println("MemberDto = " + dto);
        }
    }

    /**
     * DTO 로 가져오기 (querydsl constructor)
     */
    @Test
    public void findDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();
        for (UserDto dto : result) {
            System.out.println("UserDto = " + dto);
        }
    }

    /**
     * Dto 조회할 때 서브쿼리도 같이 사용
     */
    @Test
    public void findDtoByConstructorSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        Expressions.as(JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub), "age")
                        ))
                .from(member)
                .fetch();
        for (MemberDto dto : result) {
            System.out.println("MemberDto = " + dto);
        }
    }

    /**
     * @QueryProjection 사용
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto dto : result) {
            System.out.println("MemberDto = " + dto);
        }
    }

    /**
     * [동적 쿼리 BooleanBuilder]
     * username : member1
     * age : 10 인 사람 찾기
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * 파리미터의 값이 null 이냐에 따라서 동적으로 쿼리가 바뀌어야됨
     */
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond)); //BooleanBuilder 안에 and 조건을 추가
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond)); //BooleanBuilder 안에 and 조건을 추가
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) //builder 의 결과를 where 에 넣기
                .fetch();
    }

    /**
     * where 다중 파라미터 사용
     */
    @Test
    public void dynamicQuery_whereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null) return null;   //where() 안에 값이 null 이 오면 무시됨
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond == null) return null; //where() 안에 값이 null 이 오면 무시됨
        return member.age.eq(ageCond);
    }

    /**
     * 메서드를 조합해서 사용도 가능
     */
    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * [벌크연산]
     * 회원의 나이가 25살 미만이면 회원의 이름을 비회원으로 모두 변경
     */
    @Test
    public void bulkUpdate() {

        //영향을 받은 row 수
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(25))
                .execute();

        assertThat(count).isEqualTo(2);
    }

    /**
     * 모든 회원의 나이에 +1 하기
     */
    @Test
    public void bulkAdd() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    /**
     * 15살 초과한 모든 회원 삭제
     */
    @Test
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(15))
                .execute();
    }

    /**
     * 회원 이름에서 member 라는 단어를 M으로 바꿔서 조회
     */
    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 회원 이름을 소문자로 변경해서 비교
     */
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
