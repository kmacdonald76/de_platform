package com.dataplatform.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FlatteningInstructions implements Serializable {
    private String target;
    private Object includeParentFields; // Can be String ("all", "none") or List<String>
    private KeyAsColumn keyAsColumn;
    private Pivot pivot;
    private String arrayHandling; // "explode", "json", "ignore"
    private String objectHandling; // "flatten", "json", "ignore"
    private List<SelectField> selectFields;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Object getIncludeParentFields() {
        return includeParentFields;
    }

    public void setIncludeParentFields(Object includeParentFields) {
        this.includeParentFields = includeParentFields;
    }

    public KeyAsColumn getKeyAsColumn() {
        return keyAsColumn;
    }

    public void setKeyAsColumn(KeyAsColumn keyAsColumn) {
        this.keyAsColumn = keyAsColumn;
    }

    public Pivot getPivot() {
        return pivot;
    }

    public void setPivot(Pivot pivot) {
        this.pivot = pivot;
    }

    public String getArrayHandling() {
        return arrayHandling;
    }

    public void setArrayHandling(String arrayHandling) {
        this.arrayHandling = arrayHandling;
    }

    public String getObjectHandling() {
        return objectHandling;
    }

    public void setObjectHandling(String objectHandling) {
        this.objectHandling = objectHandling;
    }

    public List<SelectField> getSelectFields() {
        return selectFields;
    }

    public void setSelectFields(List<SelectField> selectFields) {
        this.selectFields = selectFields;
    }

    public static class KeyAsColumn implements Serializable {
        private String name;
        private String sourcePath;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }
    }

    public static class Pivot implements Serializable {
        private String keyColumnName;
        private String valuePath;

        public String getKeyColumnName() {
            return keyColumnName;
        }

        public void setKeyColumnName(String keyColumnName) {
            this.keyColumnName = keyColumnName;
        }

        public String getValuePath() {
            return valuePath;
        }

        public void setValuePath(String valuePath) {
            this.valuePath = valuePath;
        }
    }

    public static class SelectField implements Serializable {
        private String name;
        private String path;
        private String arrayHandling;
        private String objectHandling;

        // For simple field name as string
        public SelectField(String name) {
            this.name = name;
            this.path = "$." + name; // Default path for simple field names
        }

        // For complex field object
        public SelectField(
                @JsonProperty("name") String name,
                @JsonProperty("path") String path,
                @JsonProperty("arrayHandling") String arrayHandling,
                @JsonProperty("objectHandling") String objectHandling) {
            this.name = name;
            this.path = path;
            this.arrayHandling = arrayHandling;
            this.objectHandling = objectHandling;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getArrayHandling() {
            return arrayHandling;
        }

        public void setArrayHandling(String arrayHandling) {
            this.arrayHandling = arrayHandling;
        }

        public String getObjectHandling() {
            return objectHandling;
        }

        public void setObjectHandling(String objectHandling) {
            this.objectHandling = objectHandling;
        }
    }
}