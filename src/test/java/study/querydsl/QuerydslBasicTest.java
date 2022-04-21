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
        //member1 을 찾아라 -> 파라미터 바인딩 필요, 런타임 오류 발생 우려
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        //QMember m = new QMember("m");
        //QMember m = QMember.member; -> "member1"

        //member1 을 찾아라 -> 자동으로 파라미터 바인딩 처리, 컴파일 오류 발생
        //QMember.member -> static import -> member
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
    JPQL 이 제공하는 모든 검색 조건 제공

    member.username.eq("member1") // username = 'member1'
    member.username.ne("member1") //username != 'member1'
    member.username.eq("member1").not() // username != 'member1'
    member.username.isNotNull() //이름이 is not null
    member.age.in(10, 20) // age in (10,20)
    member.age.notIn(10, 20) // age not in (10, 20)
    member.age.between(10,30) //between 10, 30
    member.age.goe(30) // age >= 30
    member.age.gt(30) // age > 30
    member.age.loe(30) // age <= 30
    member.age.lt(30) // age < 30
    member.username.like("member%") //like 검색
    member.username.contains("member") // like ‘%member%’ 검색
    member.username.startsWith("member") //like ‘member%’ 검색
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
        //and 대신 쉼표로 구분 가능
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
        //리스트 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건 조회
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //페이징에서 사용(Deprecated) -> fetch() 사용 권장!, count 쿼리는 따로 작성
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetch().size();
    }

    /*
    회원 정렬 순서
    1. 회원 나이 내림차순(desc)
    2. 회원 이름 올림차순(asc)
    단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
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
        //페이징 쿼리 작성
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(3)
                .fetch();

        //count 쿼리 작성
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
    팀의 이름과 각 팀의 평균 연령을 구해라.
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
    팀 A에 소속된 모든 회원
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
    세타 조인 (연관관계가 없는 필드로 조인) -> cross join
     */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //회원의 이름이 팀 이름과 같은 회원 조회
        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //cross join 발생
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    예) 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
    JPQL : select m, t from Member m left (outer) join m.team t on t.name = 'teamA'
    SQL : SELECT m.*, t.* FROM Member m LEFT (OUTER) JOIN Team t ON m.TEAM_ID = t.id and t.name = 'teamA'

    <ON 과 WHERE 차이>
    ON : JOIN 을 하기 전 필터링을 한다 (= ON 조건으로 필터링이 된 레코드 간 JOIN 이 이뤄진다) -> null 허용
    WHERE : JOIN 을 한 후 필터링을 한다 (= JOIN 을 한 결과에서 WHERE 조건절로 필터링이 이뤄진다) -> null 걸러짐
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

        //inner join 이면 where 절을 활용하고, 정말 outer join 이 필요한 경우에만 on 절을 사용하자!!
        for (Tuple tuple : result1) {
            System.out.println("tuple = " + tuple);
        }

        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
    연관관계가 없는 엔티티 외부 조인
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //회원의 이름이 팀 이름과 같은 대상 외부 조인
        //JPQL : select m, t from Member m left join Team t on m.username = t.name
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //join 대상 바로 들어감 -> on 절 사용!!
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

        //Team 이 로드되었는지 확인 -> false
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();
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

        //Team 이 로드되었는지 확인 -> true
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /*
    서브쿼리 -> JPAExpressions 사용 -> static import
    하지만, JPA JPQL 의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.

    <해결방안>
    1. 서브쿼리를 join 으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
    2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
    3. nativeSQL 을 사용한다.
     */
    @Test
    void subQuery1() {
        QMember memberSub = new QMember("memberSub");

        //예) 나이가 가장 많은 회원 조회
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

        //예) 나이가 평균 이상인 회원 조회
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

        //예) 나이가 10살보다 큰 회원 조회 (in 절 활용)
        //JPQL : select m from Member m where m.age in (select m1.age from Member m1 where m1.age > 10) (억지 예시)
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

        //예) 회원 이름과 평균 나이 조회 (select 절 활용)
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
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
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
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타"))
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
                .select(member.username.concat("_").concat(member.age.stringValue())) //문자가 아닌 다른 타입 -> stringValue()
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
        //tuple 객체를 service 나 controller 에서 사용하는 것을 지양하자 -> dto 로 변환
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
    bean : setter 를 통해 값 주입
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
    fields : getter, setter 무시하고 필드에 바로 주입
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
    constructor : 필드 명이 달라도 주입 가능
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
    UserDto 로 조회 + 서브 쿼리 생성
     */
    @Test
    void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.constructor(
                        UserDto.class,
                        member.username,
                        ExpressionUtils.as( //서브쿼리 시작 + JPAExpressions
                                select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
    dto 생성자에 @QueryProjection 어노테이션 추가 (constructor 포함)
     */
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) //필드 타입만 맞으면 됨
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    BooleanBuilder 를 이용한 동적 쿼리 생성 -> 가독성이 상대적으로 떨어짐
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
    where 다중 파라미터를 사용하여 동적 쿼리 생성 -> 확장에 용이, 가독성 높아짐, 조합 가능!!
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
        return usernameEq(usernameCond).and(ageEq(ageCond)); //둘중 하나라도 null 이면 NPE 발생 -> null 체크 주의!!
    }

    @Test
    void bulkUpdate() {
        //member1 = 10 -> DB : member1
        //member2 = 20 -> DB : member2
        //member3 = 30 -> DB : member3
        //member4 = 40 -> DB : member4

        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //벌크 연산 후엔 반드시 영속성 컨텍스트를 flush, clear 해준다!!
//        em.flush();
//        em.clear();

        //member1 = 10 -> DB : 비회원
        //member2 = 20 -> DB : 비회원
        //member3 = 30 -> DB : member3
        //member4 = 40 -> DB : member4

        //DB 값을 조회 -> 같은 pk 값의 영속성 컨텍스트 존재 확인 -> 영속성 컨텍스트 값 조회
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
                .set(member.age, member.age.add(1)) //minus 는 없으므로 음수 추가
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
