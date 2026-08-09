package com.pet.geo.service;

import com.pet.common.BusinessException;
import com.pet.common.geo.GeoDistanceUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AmapGeoService {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    @Value("${gao.map.api-key:${GAO_MAP_API_KEY:}}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public AmapGeoService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * Resolves an address string to geographic coordinates (longitude/latitude)
     * using the AMap (Gaode Maps) geocoding API.
     * Throws BusinessException if the address is empty, the API key is missing,
     * the API call fails, or no matching location is found.
     *
     * @param address the address to geocode
     * @param label   a human-readable label for the address (used in error messages)
     * @return the resolved GeoPoint with formatted address and coordinates
     */
    public GeoPoint geocodeRequired(String address, String label) {
        String text = trimToNull(address);
        if (text == null) {
            throw new BusinessException(400, label + "不能为空");
        }
        String key = trimToNull(apiKey);
        if (key == null) {
            throw new BusinessException(400, "未配置高德地图 key，请在 .env 写入 GAO_MAP_API_KEY");
        }

        URI uri = UriComponentsBuilder.fromUriString(GEOCODE_URL)
                .queryParam("address", text)
                .queryParam("output", "json")
                .queryParam("key", key)
                .build()
                .encode()
                .toUri();

        Map<?, ?> data;
        try {
            data = restTemplate.getForObject(uri, Map.class);
        } catch (RestClientException error) {
            throw new BusinessException(400, "高德地图解析" + label + "失败，请稍后重试");
        }
        if (data == null || !"1".equals(String.valueOf(data.get("status")))) {
            String info = data == null ? null : trimToNull(String.valueOf(data.get("info")));
            throw new BusinessException(400, info == null ? "高德地图解析" + label + "失败" : info);
        }

        Object geocodesValue = data.get("geocodes");
        if (!(geocodesValue instanceof List<?> geocodes) || geocodes.isEmpty()) {
            throw new BusinessException(400, "高德地图未找到" + label + "，请重新选择更详细地址");
        }
        Object firstValue = geocodes.get(0);
        if (!(firstValue instanceof Map<?, ?> first)) {
            throw new BusinessException(400, "高德地图返回的" + label + "格式无效");
        }

        String location = trimToNull(String.valueOf(first.get("location")));
        if (location == null || !location.contains(",")) {
            throw new BusinessException(400, "高德地图未返回" + label + "位置");
        }
        String[] parts = location.split(",", 2);
        try {
            BigDecimal longitude = new BigDecimal(parts[0].trim());
            BigDecimal latitude = new BigDecimal(parts[1].trim());
            String formattedAddress = trimToNull(String.valueOf(first.get("formatted_address")));
            return new GeoPoint(formattedAddress == null ? text : formattedAddress, latitude, longitude);
        } catch (NumberFormatException error) {
            throw new BusinessException(400, "高德地图返回的" + label + "位置无效");
        }
    }

    /**
     * Calculates the great-circle distance in meters between two geographic points
     * using the Haversine formula.
     *
     * @param first  the first point
     * @param second the second point
     * @return distance in meters
     */
    public double distanceMeters(GeoPoint first, GeoPoint second) {
        return GeoDistanceUtils.distanceMeters(
                first.latitude(), first.longitude(),
                second.latitude(), second.longitude());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    public record GeoPoint(String address, BigDecimal latitude, BigDecimal longitude) {
    }
}
