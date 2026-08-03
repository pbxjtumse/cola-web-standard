package com.xjtu.iron.foundation.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertySupportTest {

    @Test
    void shouldReadJavaBeanProperty() {
        SampleBean bean = new SampleBean();
        bean.setName("foundation");
        assertEquals("foundation", PropertySupport.read(bean, "name"));
    }

    public static final class SampleBean {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
