package com.tutor.springjourney.datavalidation;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlPositionManager {
    // 存储 path -> line 的映射，例如 "clusters[0].name" -> 5
    private final Map<String, Integer> lineMap = new HashMap<>();

    public Map<String, Object> load(String content) {
        Yaml yaml = new Yaml();
        Node rootNode = yaml.compose(new StringReader(content));
        lineMap.clear();
        return (Map<String, Object>) parseNode("", rootNode);
    }

    private Object parseNode(String path, Node node) {
        if (node instanceof MappingNode) {
            Map<String, Object> map = new HashMap<>();
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
                String key = ((ScalarNode) tuple.getKeyNode()).getValue();
                String currentPath = path.isEmpty() ? key : path + "." + key;
                lineMap.put(currentPath, tuple.getKeyNode().getStartMark().getLine() + 1);
                map.put(key, parseNode(currentPath, tuple.getValueNode()));
            }
            return map;
        } else if (node instanceof SequenceNode) {
            List<Object> list = new ArrayList<>();
            List<Node> nodes = ((SequenceNode) node).getValue();
            for (int i = 0; i < nodes.size(); i++) {
                list.add(parseNode(path + "[" + i + "]", nodes.get(i)));
            }
            return list;
        } else if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        }
        return null;
    }

    public int getLine(String path) { return lineMap.getOrDefault(path, 0); }
}