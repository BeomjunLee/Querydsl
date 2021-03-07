package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static study.querydsl.entity.QMember.member;

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
}
