package net.benelog.spring.persistence

import net.benelog.spring.criteria.UserCriteria
import net.benelog.spring.criteria.UserGrade

class UserSqls {
    static String selectUserForNotice(UserCriteria criteria) {
        """
        SELECT
            user_id, email
        FROM
            nm_usr
        WHERE
            user_grade = :crteria.grade
        ${
            if (criteria.grade == UserGrade.) {
                """
                    AND user.last_login < :criteria.loginDateLimit
                """
            } else {
                """
                    AND user.last_login > :criteria.loginDateLimit
                """
            }
        }
        """
    }
}
