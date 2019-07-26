open module ghost2.main {
    requires java.persistence;
    requires java.validation;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.data.commons;
    requires spring.data.jpa;
    requires spring.context;
    requires org.apache.commons.lang3;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.commons.compress;
    requires org.apache.commons.io;
    requires discord4j.core;
    requires discord4j.rest;
    requires reactor.core;
    requires org.reactivestreams;
    requires tinylog;
    requires owner;
    requires reflections;
    requires jackson.annotations;
    requires java.annotation;
    requires spring.web;
    requires spring.core;
}