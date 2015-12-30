package jenkins.plugins.bearychat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

public class StandardBearychatService implements BearychatService {

    private static final Logger logger = Logger.getLogger(StandardBearychatService.class.getName());

    // TODO: rm stage
    private String host = "local.bearychat.com";
    private String teamDomain;
    private String token;
    private String[] roomIds;

    public StandardBearychatService(String teamDomain, String token, String roomId) {
        super();
        this.teamDomain = teamDomain;
        this.token = token;
        this.roomIds = roomId.split(",");
    }

    public void publish(String message) {
        publish(message, "warning");
    }

    public void publish(String message, String color) {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("message", message);
        dataMap.put("color", color);
        publish("unknown", dataMap);
    }

    @Override
    public void publish(String action, Map<String, Object> dataMap) {
        for (String roomId : roomIds) {
            String url = "https://" + teamDomain + "." + host + "/api/hooks/jenkins/" + token;
            logger.info("Posting: to " + roomId + " on " + teamDomain + " using " + url +": " + dataMap);
            HttpClient client = getHttpClient();
            PostMethod post = new PostMethod(url);
            JSONObject json = new JSONObject();

            try {

                JSONObject dataJson = new JSONObject();

                String message = "", color = "";
                if(dataMap != null && !dataMap.isEmpty()){
                    message = (String)dataMap.get("message");
                    color = (String)dataMap.get("color");

                    dataJson.put("authors", dataMap.get("authors"));
                    dataJson.put("files", dataMap.get("files"));

                    Map<String,String> configMap = (Map<String,String>)dataMap.get("config");
                    JSONObject configJson = new JSONObject();
                    for(String key : configMap.keySet()){
                        Object val = configMap.get(key);
                        configJson.put(key, val);
                    }
                    dataJson.put("config", configJson);

                    Map<String,String> projectMap = (Map<String,String>)dataMap.get("project");
                    JSONObject projectJson = new JSONObject();
                    for(String key : projectMap.keySet()){
                        Object val = projectMap.get(key);
                        projectJson.put(key, val);
                    }
                    dataJson.put("project", projectJson);

                    Map<String,String> jobMap = (Map<String,String>)dataMap.get("job");
                    JSONObject jobJson = new JSONObject();
                    for(String key : jobMap.keySet()){
                        Object val = jobMap.get(key);
                        jobJson.put(key, val);
                    }
                    dataJson.put("job", jobJson);
                }
                String data = dataJson.toString();


                json.put("action", action);
                json.put("channel", roomId);
                json.put("text", message);
                json.put("color", color);
                json.put("data", data);

                post.addParameter("payload", json.toString());
                post.getParams().setContentCharset("UTF-8");

                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Bearychat post may have failed. Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Bearychat", e);
            } finally {
                logger.info("Posting succeeded");
                post.releaseConnection();
            }
        }
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
                    // and
                    // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
                    client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }

    void setHost(String host) {
        this.host = host;
    }

}
