package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class YamlDigestHelper {

	public static final Map<Class<?>, Tag> YAML_CLASS_TAGS;
	
	static {
		YAML_CLASS_TAGS = new HashMap<Class<?>, Tag>();
		
		YAML_CLASS_TAGS.put(SSCAEProjectDigest.class, new Tag("!SSCAE.Digest"));
		YAML_CLASS_TAGS.put(ProjectClassification.class, new Tag("!SSCAE.Classification"));
		YAML_CLASS_TAGS.put(URI.class, new Tag("!emf.URI"));
		YAML_CLASS_TAGS.put(ElementProxyInfo.class, new Tag("!SSCAE.Proxy"));
		YAML_CLASS_TAGS.put(MDLocalPrimaryProject.class, new Tag("!SSCAE.LocalPrimaryProjectVertex"));
		YAML_CLASS_TAGS.put(MDLocalAttachedProject.class, new Tag("!SSCAE.LocalAttachedProjectVertex"));
		YAML_CLASS_TAGS.put(MDTeamworkPrimaryProject.class, new Tag("!SSCAE.TeamworkPrimaryProjectVertex"));
		YAML_CLASS_TAGS.put(MDTeamworkAttachedProject.class, new Tag("!SSCAE.TeamworkAttachedProjectVertex"));
		YAML_CLASS_TAGS.put(MDLocalProjectUsage.class, new Tag("!SSCAE.LocalProjectEdge"));
		YAML_CLASS_TAGS.put(MDTeamworkProjectUsage.class, new Tag("!SSCAE.TeamworkProjectEdge"));
		YAML_CLASS_TAGS.put(DiagramProxyUsageProblems.class, new Tag("!SSCAE.DiagramProxies"));
		
	}
	
	public static DumperOptions createDumperOptions() {
		DumperOptions options = new DumperOptions();
		return options;
	}
	
	public static void configureRepresenter(Representer r) {
		for (Map.Entry<Class<?>, Tag> entry : YAML_CLASS_TAGS.entrySet()) {
			r.addClassTag(entry.getKey(), entry.getValue());
		}
	}

	public static void configureConstructors(DigestConstructor c) {
		for (Map.Entry<Class<?>, Tag> entry : YAML_CLASS_TAGS.entrySet()) {
			c.addTypeDescription(new TypeDescription(entry.getKey(), entry.getValue()));
		}
	}
	
	public static String serialize(SSCAEProjectDigest digest) {
		
		Representer representer = new DigestRepresenter();
		DumperOptions options = YamlDigestHelper.createDumperOptions();
		Yaml yaml = new Yaml(representer, options);
		String serialization = yaml.dump(digest);
		return serialization;
	}
	
	public static SSCAEProjectDigest load(String serialization) throws YAMLException {

		Representer representer = new DigestRepresenter();
		DumperOptions options = YamlDigestHelper.createDumperOptions();
		Constructor constructor = new DigestConstructor();
		Yaml yamLoader = new Yaml(constructor, representer, options);

		SSCAEProjectDigest digest = (SSCAEProjectDigest) yamLoader.load(serialization);
		return digest;
	}
}
