package com.tinyroute.client;

import com.tinyroute.model.OAuthTransaction;
import com.tinyroute.model.GoogleIdentity;

import java.net.URI;

public interface GoogleOAuthClient {

    URI authorizationUri(String state, OAuthTransaction transaction);

    GoogleIdentity exchangeAuthorizationCode(String code, OAuthTransaction transaction);
}
