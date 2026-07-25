package Caching_Proxy.cachingProxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class CachingProxyApplication {

	public static void main(String[] args) {
		Map<String, String> parsed = parsedArgs(args);
		if(parsed.containsKey("clear-cache")){
			String port = parsed.get("port");
			try{
				RestClient.create()
						.post()
						.uri("http://localhost:" + port + "/clear")
						.retrieve()
						.toBodilessEntity();
			} catch (Exception e){
				System.err.println("Could not reach a running server on port " + port
						+ " — is it started?");
			}
			return;
		}
		String port = parsed.get("port");
		String origin = parsed.get("origin");
		if(port==null || origin==null){
			throw new IllegalArgumentException("Usage: --port <port> --origin <origin>");
		}
		Map<String, Object> props = new HashMap<>();
		props.put("server.port", port);
		props.put("proxy.origin", origin);
		new SpringApplicationBuilder(CachingProxyApplication.class)
				.properties(props)
				.run(args);
	}

	private static Map<String, String> parsedArgs(String[] args) {
		Map<String, String> parsed = new HashMap<>();
		for(int i=0; i<args.length; i++){
			String word = args[i];
			if(!word.startsWith("--")){
				continue;
			}
			String key = word.substring(2);
			String value = null;
			if(i+1<args.length){
				value = args[i+1];
			}
			parsed.put(key, value);
		}
		return parsed;
	}

}
