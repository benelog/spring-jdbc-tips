package net.benelog.spring.persistence

import net.benelog.spring.criteria.UserCriteria
import net.benelog.spring.criteria.UserGrade
import org.junit.Test

class UserSqlsTest {
    @Test
    void testSelectUserForNotice() {

        System.out.println(UserSqls.selectUserForNotice(new UserCriteria(UserGrade.SPECIAL)));
        System.out.println(UserSqls.selectUserForNotice(new UserCriteria(UserGrade.LOCKED)));
    }
}
