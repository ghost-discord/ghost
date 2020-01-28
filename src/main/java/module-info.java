@SuppressWarnings("JavaModuleNaming")
open module ghost2.main {
    requires java.annotation;
    requires java.validation;
    requires java.persistence;
    requires java.sql;
    requires spring.core;
    requires spring.web;
    requires spring.orm;
    requires spring.boot;
    requires spring.beans;
    requires spring.context;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires spring.boot.autoconfigure;
    requires com.zaxxer.hikari;
    requires org.apache.commons.lang3;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.commons.compress;
    requires org.apache.commons.io;
    requires discord4j.core;
    requires discord4j.rest;
    requires discord4j.voice;
    requires lavaplayer;
    requires reactor.core;
    requires org.reactivestreams;
    requires io.netty.handler;
    requires io.netty.codec.http;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires reflections;
    requires tinylog;
    requires owner;
    requires org.jsoup;
}