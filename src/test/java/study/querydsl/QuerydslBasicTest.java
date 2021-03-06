package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired EntityManager em;
    @PersistenceUnit EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void beforeEach() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member(teamA, "member1", 10);
        Member member2 = new Member(teamA, "member2", 20);
        Member member3 = new Member(teamB, "member3", 30);
        Member member4 = new Member(teamB, "member4", 40);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        //member1 ??? ????????? -> ???????????? ????????? ??????, ????????? ?????? ?????? ??????
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        //QMember m = new QMember("m");
        //QMember m = QMember.member; -> "member1"

        //member1 ??? ????????? -> ???????????? ???????????? ????????? ??????, ????????? ?????? ??????
        //QMember.member -> static import -> member
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
    JPQL ??? ???????????? ?????? ?????? ?????? ??????

    member.username.eq("member1") // username = 'member1'
    member.username.ne("member1") //username != 'member1'
    member.username.eq("member1").not() // username != 'member1'
    member.username.isNotNull() //????????? is not null
    member.age.in(10, 20) // age in (10,20)
    member.age.notIn(10, 20) // age not in (10, 20)
    member.age.between(10,30) //between 10, 30
    member.age.goe(30) // age >= 30
    member.age.gt(30) // age > 30
    member.age.loe(30) // age <= 30
    member.age.lt(30) // age < 30
    member.username.like("member%") //like ??????
    member.username.contains("member") // like ???%member%??? ??????
    member.username.startsWith("member") //like ???member%??? ??????
    ...
     */

    @Test
    void search() {
        //select m from Member m where m.username = 'member1' and m.age = 10
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        //and ?????? ????????? ?????? ??????
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
        //????????? ??????
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //??? ??? ??????
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        //?????? ??? ??? ??????
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //??????????????? ??????(Deprecated) -> fetch() ?????? ??????!, count ????????? ?????? ??????
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        //count ????????? ??????
        long count = queryFactory
                .selectFrom(member)
                .fetch().size();
    }

    /*
    ?????? ?????? ??????
    1. ?????? ?????? ????????????(desc)
    2. ?????? ?????? ????????????(asc)
    ???, 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
     */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging() {
        //????????? ?????? ??????
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(3)
                .fetch();

        //count ?????? ??????
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        assertThat(result.size()).isEqualTo(3);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void aggregation() {
        Tuple tuple = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetchOne();

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
    ?????? ????????? ??? ?????? ?????? ????????? ?????????.
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name) //col_0 : team.name, col_1 : member.age.avg
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
    }

    /*
    ??? A??? ????????? ?????? ??????
     */
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //join member.team (as) team
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
    ?????? ?????? (??????????????? ?????? ????????? ??????) -> cross join
     */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //????????? ????????? ??? ????????? ?????? ?????? ??????
        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //cross join ??????
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    ???) ????????? ?????? ???????????????, ??? ????????? teamA ??? ?????? ??????, ????????? ?????? ??????
    JPQL : select m, t from Member m left (outer) join m.team t on t.name = 'teamA'
    SQL : SELECT m.*, t.* FROM Member m LEFT (OUTER) JOIN Team t ON m.TEAM_ID = t.id and t.name = 'teamA'

    <ON ??? WHERE ??????>
    ON : JOIN ??? ?????? ??? ???????????? ?????? (= ON ???????????? ???????????? ??? ????????? ??? JOIN ??? ????????????) -> null ??????
    WHERE : JOIN ??? ??? ??? ???????????? ?????? (= JOIN ??? ??? ???????????? WHERE ???????????? ???????????? ????????????) -> null ?????????
     */
    @Test
    void join_on_filtering() {
        //outer join
        List<Tuple> result1 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //inner join
        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //inner join ?????? where ?????? ????????????, ?????? outer join ??? ????????? ???????????? on ?????? ????????????!!
        for (Tuple tuple : result1) {
            System.out.println("tuple = " + tuple);
        }

        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
    ??????????????? ?????? ????????? ?????? ??????
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
        //JPQL : select m, t from Member m left join Team t on m.username = t.name
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //join ?????? ?????? ????????? -> on ??? ??????!!
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //Team ??? ?????????????????? ?????? -> false
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
    }

    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //Team ??? ?????????????????? ?????? -> true
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("?????? ?????? ??????").isTrue();
    }

    /*
    ???????????? -> JPAExpressions ?????? -> static import
    ?????????, JPA JPQL ??? ??????????????? from ?????? ????????????(????????? ???)??? ???????????? ?????????.

    <????????????>
    1. ??????????????? join ?????? ????????????. (????????? ????????? ??????, ???????????? ????????? ??????.)
    2. ???????????????????????? ????????? 2??? ???????????? ????????????.
    3. nativeSQL ??? ????????????.
     */
    @Test
    void subQuery1() {
        QMember memberSub = new QMember("memberSub");

        //???) ????????? ?????? ?????? ?????? ??????
        //JPQL : select m from Member m where m.age = (select max(m1.age) from Member m1)
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max()) //JPAExpressions
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    @Test
    void subQuery2() {
        QMember memberSub = new QMember("memberSub");

        //???) ????????? ?????? ????????? ?????? ??????
        //JPQL : select m from Member m where m.age >= (select avg(m1.age) from Member m1)
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg()) //JPAExpressions
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    void subQuery3() {
        QMember memberSub = new QMember("memberSub");

        //???) ????????? 10????????? ??? ?????? ?????? (in ??? ??????)
        //JPQL : select m from Member m where m.age in (select m1.age from Member m1 where m1.age > 10) (?????? ??????)
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age) //JPAExpressions
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void subQuery4() {
        QMember memberSub = new QMember("memberSub");

        //???) ?????? ????????? ?????? ?????? ?????? (select ??? ??????)
        //JPQL : select m.username, (select avg(m1.age) from Member m1) from Member m
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg()) //JPAExpressions
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21???~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        //{username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) //????????? ?????? ?????? ?????? -> stringValue()
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
        //tuple ????????? service ??? controller ?????? ???????????? ?????? ???????????? -> dto ??? ??????
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

    @Test
    void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    bean : setter ??? ?????? ??? ??????
     */
    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(
                        MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    fields : getter, setter ???????????? ????????? ?????? ??????
     */
    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(
                        MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    constructor : ?????? ?????? ????????? ?????? ??????
     */
    @Test
    void findDtoByConstructor() {
        //MemberDto -> username, age
        List<MemberDto> result1 = queryFactory
                .select(Projections.constructor(
                        MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        //UserDto -> name, age
        List<UserDto> result2 = queryFactory
                .select(Projections.constructor(
                        UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result1) {
            System.out.println("memberDto = " + memberDto);
        }

        for (UserDto userDto : result2) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
    UserDto ??? ?????? + ?????? ?????? ??????
     */
    @Test
    void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.constructor(
                        UserDto.class,
                        member.username,
                        ExpressionUtils.as( //???????????? ?????? + JPAExpressions
                                select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
    dto ???????????? @QueryProjection ??????????????? ?????? (constructor ??????)
     */
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) //?????? ????????? ????????? ???
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    BooleanBuilder ??? ????????? ?????? ?????? ?????? -> ???????????? ??????????????? ?????????
     */
    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /*
    where ?????? ??????????????? ???????????? ?????? ?????? ?????? -> ????????? ??????, ????????? ?????????, ?????? ??????!!
     */
    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
//        String usernameParam = null;
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond)); //?????? ???????????? null ?????? NPE ?????? -> null ?????? ??????!!
    }

    @Test
    void bulkUpdate() {
        //member1 = 10 -> DB : member1
        //member2 = 20 -> DB : member2
        //member3 = 30 -> DB : member3
        //member4 = 40 -> DB : member4

        queryFactory
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        //?????? ?????? ?????? ????????? ????????? ??????????????? flush, clear ?????????!!
//        em.flush();
//        em.clear();

        //member1 = 10 -> DB : ?????????
        //member2 = 20 -> DB : ?????????
        //member3 = 30 -> DB : member3
        //member4 = 40 -> DB : member4

        //DB ?????? ?????? -> ?????? pk ?????? ????????? ???????????? ?????? ?????? -> ????????? ???????????? ??? ??????
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member1 = " + member);
        }
    }

    @Test
    void bulkAddAndMultiply() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //minus ??? ???????????? ?????? ??????
                .execute();

        queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    void sqlFunction1() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {
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
