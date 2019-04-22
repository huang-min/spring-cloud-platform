package com.mindasoft.cloud.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindasoft.cloud.security.util.OAuth2Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.*;
import org.springframework.security.oauth2.provider.error.DefaultWebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author owen 624191343@qq.com
 * @version 创建时间：2017年11月12日 上午22:57:51
 */
@Slf4j
@Configuration
public class SecurityHandlerConfig {

	@Resource
	private ObjectMapper objectMapper; // springmvc启动时自动装配json处理类
	@Autowired
	private TokenStore tokenStore;

	/**
	 * 登陆成功，返回Token 装配此bean不支持授权码模式
	 * 
	 * @return
	 */
	@Bean
	public AuthenticationSuccessHandler loginSuccessHandler() {
		return new SavedRequestAwareAuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
												Authentication authentication) throws IOException, ServletException {
				System.out.println(request.getRequestURL());
				super.onAuthenticationSuccess(request, response, authentication);
			}
		};
	}

	/**
	 * 登陆失败
	 * 
	 * @return
	 */
	@Bean
	public AuthenticationFailureHandler loginFailureHandler() {
		return new AuthenticationFailureHandler() {

			@Override
			public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
				String msg = null;
				if (exception instanceof BadCredentialsException) {
					msg = "密码错误";
				} else {
					msg = exception.getMessage();
				}

				Map<String, String> rsp = new HashMap<>();

				response.setStatus(HttpStatus.UNAUTHORIZED.value());

				rsp.put("code", HttpStatus.UNAUTHORIZED.value() + "");
				rsp.put("msg", msg);

				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write(objectMapper.writeValueAsString(rsp));
				response.getWriter().flush();
				response.getWriter().close();

			}
		};

	}

	/**
	 * 退出后，删除token
	 * @return
	 */
	@Bean
	public LogoutHandler oauthLogoutHandler(){
		return new LogoutHandler(){
			@Override
			public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
				Assert.notNull(tokenStore, "tokenStore must be set");
				String token = OAuth2Utils.extractToken(request);
				if(StringUtils.isNotBlank(token)){
					OAuth2AccessToken existingAccessToken = tokenStore.readAccessToken(token);
					OAuth2RefreshToken refreshToken;
					if (existingAccessToken != null) {
						if (existingAccessToken.getRefreshToken() != null) {
							log.info("remove refreshToken!", existingAccessToken.getRefreshToken());
							refreshToken = existingAccessToken.getRefreshToken();
							tokenStore.removeRefreshToken(refreshToken);
						}
						log.info("remove existingAccessToken!", existingAccessToken);
						tokenStore.removeAccessToken(existingAccessToken);
					}
				}
			}
		};
	}

	@Bean
	public WebResponseExceptionTranslator webResponseExceptionTranslator() {
		return new DefaultWebResponseExceptionTranslator() {
			public static final String BAD_MSG = "坏的凭证";
			@Override
			public ResponseEntity<OAuth2Exception> translate(Exception e) throws Exception {
				OAuth2Exception oAuth2Exception;
				if (e.getMessage() != null && e.getMessage().equals(BAD_MSG)) {
					oAuth2Exception = new InvalidGrantException("用户名或密码错误", e);
				} else if (e instanceof InternalAuthenticationServiceException) {
					oAuth2Exception = new InvalidGrantException(e.getMessage(), e);
				} else if (e instanceof RedirectMismatchException) {
					oAuth2Exception = new InvalidGrantException(e.getMessage(), e);
				} else if (e instanceof InvalidScopeException) {
					oAuth2Exception = new InvalidGrantException(e.getMessage(), e);
				} else {
					oAuth2Exception = new UnsupportedResponseTypeException("服务内部错误", e);
				}

				ResponseEntity<OAuth2Exception> response = super.translate(oAuth2Exception);
				ResponseEntity.status(oAuth2Exception.getHttpErrorCode());
				response.getBody().addAdditionalInformation("code", oAuth2Exception.getHttpErrorCode() + "");
				response.getBody().addAdditionalInformation("msg", oAuth2Exception.getMessage());

				return response;
			}

		};
	}

}
