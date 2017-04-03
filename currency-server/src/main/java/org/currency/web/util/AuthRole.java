package org.currency.web.util;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthRole {

    public static final String  ADMIN = "ADMIN";
    public static final String  USER = "USER";

    public static final Set<String> ADMIN_ROLES =  Collections.unmodifiableSet(new HashSet<>(Arrays.asList("USER", "ADMIN")));
    public static final Set<String> USER_ROLES =  Collections.unmodifiableSet(new HashSet<>(Arrays.asList("USER")));

}
