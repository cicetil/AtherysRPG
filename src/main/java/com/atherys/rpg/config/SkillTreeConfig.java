package com.atherys.rpg.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ConfigSerializable
public class SkillTreeConfig {

    @Setting("skill-nodes")
    public Map<String, SkillNodeConfig> SKILL_NODES = new HashMap<>();

    @Setting("skill-links")
    public Set<SkillNodeLinkConfig> SKILL_LINKS = new HashSet<>();

}
