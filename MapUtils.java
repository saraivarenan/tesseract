

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapUtils {

  private MapUtils() {
  }

  public static Map<String, Object> mergeMaps(Map<String, Object> base,
      Map<String, Object> compose) {
    compose.forEach((key, value) -> {
      Object valueBase = base.get(key);

      if (value instanceof Map && valueBase instanceof Map) {
        base.put(key, mergeMaps((Map<String, Object>) valueBase, (Map<String, Object>) value));
      } else {
        base.put(key, value);
      }
    });

    return base;
  }

  public static Map<String, Object> resolveParameters(Map<String, Object> values) {
    return resolveParameters(values, values);
  }

  private static Map<String, Object> resolveParameters(Map<String, Object> values,
      Map<String, Object> root) {
    Pattern p = Pattern.compile("\\$\\{([^\\}\\{]*)\\}");

    values.forEach((key, value) -> {
      if (value instanceof String) {
        Matcher matcher = p.matcher((String) value);

        StringBuffer builder = new StringBuffer();
        boolean entrou = false;

        while (matcher.find()) {
          entrou = true;

          String param = matcher.group(1);

          String paramValue = getParam(root, param.split("\\."));

          matcher.appendReplacement(builder, paramValue == null ? "" : paramValue);
        }

        matcher.appendTail(builder);

        values.put(key, entrou ? builder.toString() : value);
      } else if (value instanceof Map) {
        values.put(key, resolveParameters((Map<String, Object>) value, root));
      }
    });

    return values;
  }

  private static String getParam(Map<String, Object> values, String[] param) {
    Object value = values.get(param[0]);

    if (param.length > 1) {
      String[] arr = subArray(param, 1, param.length - 1);

      value = getParam((Map) value, arr);
    }

    return (String) value;
  }

  public static String[] subArray(String[] array, int beg, int end) {
    return IntStream.range(beg, end + 1)
        .mapToObj(i -> array[i])
        .toArray(String[]::new);
  }

  public static Map<String, Object> resolveToNestedProps(Map<String, Object> props) {
    return resolveToNestedProps(props, null, new HashMap<String, Object>());
  }

  private static Map<String, Object> resolveToNestedProps(Map<String, Object> props, String sProp,
      Map<String, Object> valueMap) {
    props.forEach((key, vl) -> {
      String name = (sProp == null ? "" : sProp) + (sProp == null ? "" : ".") + key;

			if (vl instanceof Map) {
				resolveToNestedProps((Map) vl, name, valueMap);
			} else {
				valueMap.put(name, vl);
			}
    });

    return valueMap;
  }
}
