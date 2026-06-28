package com.clinic.common.constant;

public class Constants {

    public static final String TOKEN = "token";

    public static final String USER_INFO = "userInfo";

    public static final Long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    public static final String JWT_SECRET = "clinicSaasSecretKey2024";

    public static final String JWT_ISSUER = "clinic-saas";

    public static final String REDIS_USER_PREFIX = "user:token:";

    public static final String REDIS_CAPTCHA_PREFIX = "captcha:";

    public static final Integer SUPER_ADMIN_CLINIC_ID = 1;

    public static final Integer DEFAULT_PAGE_NUM = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final Integer MAX_PAGE_SIZE = 100;
}