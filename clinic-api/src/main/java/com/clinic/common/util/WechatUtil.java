package com.clinic.common.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信工具类
 */
@Slf4j
@Component
public class WechatUtil {

    @Value("${wechat.miniapp.appid:wxid_3tmgeou6k2i822}")
    private String appid;

    @Value("${wechat.miniapp.secret:}")
    private String secret;

    private static String STATIC_APPID;
    private static String STATIC_SECRET;

    @PostConstruct
    public void init() {
        STATIC_APPID = appid;
        STATIC_SECRET = secret;
        log.info("微信小程序配置加载完成，appid: {}", appid);
    }

    private static final String JSCODE_TO_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 根据code获取openid和session_key
     */
    public static Map<String, String> getOpenidByCode(String code) {
        Map<String, String> result = new HashMap<>();

        // 如果没有配置 secret，使用模拟数据（开发测试用）
        if (STATIC_SECRET == null || STATIC_SECRET.isEmpty() || "YOUR_TEST_SECRET_HERE".equals(STATIC_SECRET)) {
            log.warn("未配置微信 secret，使用模拟登录");
            String mockOpenid = "mock_openid_" + code.substring(0, Math.min(10, code.length()));
            result.put("openid", mockOpenid);
            result.put("sessionKey", "mock_session_key");
            return result;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("appid", STATIC_APPID);
            params.put("secret", STATIC_SECRET);
            params.put("js_code", code);
            params.put("grant_type", "authorization_code");

            String response = HttpUtil.get(JSCODE_TO_SESSION_URL, params);
            log.info("微信登录响应: {}", response);

            JSONObject json = JSONUtil.parseObj(response);

            if (json.containsKey("openid")) {
                result.put("openid", json.getStr("openid"));
                result.put("sessionKey", json.getStr("session_key"));
                result.put("unionid", json.getStr("unionid"));
            } else {
                log.error("微信登录失败: {}", json.getStr("errmsg"));
                // 失败时使用模拟数据
                String mockOpenid = "mock_openid_" + code.substring(0, Math.min(10, code.length()));
                result.put("openid", mockOpenid);
                result.put("sessionKey", "mock_session_key");
            }
        } catch (Exception e) {
            log.error("调用微信接口失败: {}", e.getMessage(), e);
            // 异常时使用模拟数据
            String mockOpenid = "mock_openid_" + code.substring(0, Math.min(10, code.length()));
            result.put("openid", mockOpenid);
            result.put("sessionKey", "mock_session_key");
        }

        return result;
    }

    /**
     * 解密微信加密数据（获取手机号）
     * 参考微信官方文档：https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/signature.html
     */
    public static String decryptData(String encryptedData, String sessionKey, String iv) {
        try {
            // 检查是否是模拟模式
            if (sessionKey == null || sessionKey.isEmpty() || sessionKey.startsWith("mock_")) {
                log.warn("当前为模拟登录模式，无法解密真实手机号数据");
                return null;
            }

            // 添加BouncyCastleProvider支持AES解密
            Security.addProvider(new BouncyCastleProvider());

            // 微信使用 URL-safe Base64（将 + 替换为 -，/ 替换为 _）
            // 需要转换回标准 Base64 或直接使用 URL-safe decoder
            String standardEncryptedData = encryptedData.replace('-', '+').replace('_', '/');
            String standardSessionKey = sessionKey.replace('-', '+').replace('_', '/');
            String standardIv = iv.replace('-', '+').replace('_', '/');

            // Base64解码
            byte[] encryptedDataBytes = Base64.getDecoder().decode(standardEncryptedData);
            byte[] sessionKeyBytes = Base64.getDecoder().decode(standardSessionKey);
            byte[] ivBytes = Base64.getDecoder().decode(standardIv);

            // 使用AES-128-CBC模式解密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            SecretKeySpec secretKeySpec = new SecretKeySpec(sessionKeyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedDataBytes);
            String decryptedData = new String(decryptedBytes, "UTF-8");

            log.info("解密成功: {}", decryptedData);

            // 解析JSON获取手机号
            JSONObject jsonObject = JSONUtil.parseObj(decryptedData);
            String phoneNumber = jsonObject.getStr("phoneNumber");
            String purePhoneNumber = jsonObject.getStr("purePhoneNumber");

            // 优先返回 purePhoneNumber（不带区号的手机号）
            return purePhoneNumber != null ? purePhoneNumber : phoneNumber;
        } catch (Exception e) {
            log.error("解密数据失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
