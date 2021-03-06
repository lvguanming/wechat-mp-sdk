package org.usc.wechat.mp.sdk.util;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usc.wechat.mp.sdk.util.platform.AccessTokenUtil;
import org.usc.wechat.mp.sdk.vo.JsonRtn;
import org.usc.wechat.mp.sdk.vo.WechatRequest;
import org.usc.wechat.mp.sdk.vo.token.License;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 * @author Shunli
 */
public class HttpUtil {
    private final static Logger log = LoggerFactory.getLogger(HttpUtil.class);
    private static final Function<Map.Entry<String, String>, NameValuePair> nameValueTransformFunction = new Function<Map.Entry<String, String>, NameValuePair>() {
        @Override
        public NameValuePair apply(final Map.Entry<String, String> input) {
            return new BasicNameValuePair(input.getKey(), input.getValue());
        }
    };

    /**
     * handle response's entity to utf8 text
     */
    public static final ResponseHandler<String> UTF8_CONTENT_HANDLER = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, "UTF-8");
            }

            return StringUtils.EMPTY;
        }
    };

    public static <T extends JsonRtn> T getRequest(WechatRequest request, License license, Class<T> jsonRtnClazz) {
        return getRequest(request, license, null, jsonRtnClazz);
    }

    public static <T extends JsonRtn> T getRequest(WechatRequest request, License license, Map<String, String> paramMap, Class<T> jsonRtnClazz) {
        String requestUrl = request.getUrl();
        String requestName = request.getName();
        List<NameValuePair> nameValuePairs = buildNameValuePairs(license, paramMap);

        try {
            URI uri = new URIBuilder(requestUrl).setParameters(nameValuePairs).build();

            String json = Request.Get(uri).execute().handleResponse(HttpUtil.UTF8_CONTENT_HANDLER);
            T jsonRtn = JsonRtnUtil.parseJsonRtn(json, jsonRtnClazz);
            log.info(requestName + " result:\n url={},\n rtn={},\n {}", uri, json, jsonRtn);
            return jsonRtn;
        } catch (Exception e) {
            String msg = requestName + " failed:\n url=" + requestUrl + "\n params=" + nameValuePairs;
            log.error(msg, e);
            return null;
        }
    }

    public static <T extends JsonRtn> T postBodyRequest(WechatRequest request, License license, Object requestBody, Class<T> jsonRtnClazz) {
        return postBodyRequest(request, license, null, requestBody, jsonRtnClazz);
    }

    public static <T extends JsonRtn> T postBodyRequest(WechatRequest request, License license, Map<String, String> paramMap, Object requestBody, Class<T> jsonRtnClazz) {
        if (request == null || license == null || requestBody == null || jsonRtnClazz == null) {
            return null;
        }

        String requestUrl = request.getUrl();
        String requestName = request.getName();
        List<NameValuePair> nameValuePairs = buildNameValuePairs(license, paramMap);
        String body = JSONObject.toJSONString(requestBody);

        try {
            URI uri = new URIBuilder(requestUrl).setParameters(nameValuePairs).build();

            String rtnJson = Request.Post(uri)
                    .bodyString(body, ContentType.create("text/html", Consts.UTF_8))
                    .execute().handleResponse(HttpUtil.UTF8_CONTENT_HANDLER);

            T jsonRtn = JsonRtnUtil.parseJsonRtn(rtnJson, jsonRtnClazz);
            log.info(requestName + " result:\n url={},\n body={},\n rtn={},\n {}", uri, body, rtnJson, jsonRtn);
            return jsonRtn;
        } catch (Exception e) {
            String msg = requestName + " failed:\n url=" + requestUrl + "\n params=" + nameValuePairs + ",\n body=" + body;
            log.error(msg, e);
            return null;
        }
    }

    private static List<NameValuePair> buildNameValuePairs(License license, Map<String, String> paramMap) {
        List<NameValuePair> nameValuePairs = Lists.newArrayList();
        nameValuePairs.add(new BasicNameValuePair("access_token", AccessTokenUtil.getAccessToken(license)));
        if (paramMap != null) {
            Iterables.addAll(nameValuePairs, Iterables.transform(paramMap.entrySet(), nameValueTransformFunction));
        }
        return nameValuePairs;
    }
}
