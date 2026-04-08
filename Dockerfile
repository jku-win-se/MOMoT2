FROM maven:3.9-eclipse-temurin AS build
WORKDIR /src
COPY . .
ENV MAVEN_OPTS="-Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0"
RUN mvn -pl tooling/at.ac.tuwien.big.momot.tooling,plugins/at.ac.tuwien.big.moea,plugins/at.ac.tuwien.big.momot.core,plugins/at.ac.tuwien.big.momot.runner -am -Declipse.release=latest -DskipTests=true clean package
RUN mkdir -p /app/repository/plugins
RUN cp /src/plugins/at.ac.tuwien.big.momot.runner/target/at.ac.tuwien.big.momot.runner-*.jar /app/repository/plugins/
RUN cp /src/plugins/at.ac.tuwien.big.moea/target/at.ac.tuwien.big.moea-*.jar /app/repository/plugins/
RUN cp /src/plugins/at.ac.tuwien.big.moea/lib/*.jar /app/repository/plugins/
RUN cp /src/plugins/at.ac.tuwien.big.momot.core/target/at.ac.tuwien.big.momot.core-*.jar /app/repository/plugins/
RUN cp /src/plugins/at.ac.tuwien.big.momot.lang/target/at.ac.tuwien.big.momot.lang-*.jar /app/repository/plugins/
RUN set -eux; \
	for bundle in org.eclipse.ocl org.eclipse.ocl.common org.eclipse.ocl.ecore lpg.runtime.java; do \
		jar_path="$(find /root/.m2/repository/p2/osgi/bundle/$bundle -name '*.jar' | sort -V | tail -n 1)"; \
		if [ -z "$jar_path" ]; then \
			echo "Missing required OCL bundle jar for $bundle" >&2; \
			exit 1; \
		fi; \
		cp "$jar_path" /app/repository/plugins/; \
	done
RUN mvn -pl plugins/at.ac.tuwien.big.momot.runner -DskipTests=true -DincludeScope=runtime dependency:copy-dependencies -DoutputDirectory=/app/repository/plugins
RUN find /app/repository/plugins -type f \( -name '*-sources.jar' -o -name '*-javadoc.jar' \) -delete
RUN rm -f /app/repository/plugins/guava-18.0.jar
RUN set -eux; \
	base_url="https://download.eclipse.org/modeling/emft/henshin/updates/release/plugins"; \
	curl -fsSL "$base_url/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar" -o "/app/repository/plugins/org.eclipse.emf.henshin.model_1.8.0.202302121604.jar"; \
	curl -fsSL "$base_url/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar" -o "/app/repository/plugins/org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar"
RUN set -eux; \
	base_url="https://repo1.maven.org/maven2"; \
	curl -fsSL "$base_url/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar" -o "/app/repository/plugins/nashorn-core-15.4.jar"; \
	curl -fsSL "$base_url/org/ow2/asm/asm/7.3.1/asm-7.3.1.jar" -o "/app/repository/plugins/asm-7.3.1.jar"; \
	curl -fsSL "$base_url/org/ow2/asm/asm-commons/7.3.1/asm-commons-7.3.1.jar" -o "/app/repository/plugins/asm-commons-7.3.1.jar"; \
	curl -fsSL "$base_url/org/ow2/asm/asm-tree/7.3.1/asm-tree-7.3.1.jar" -o "/app/repository/plugins/asm-tree-7.3.1.jar"; \
	curl -fsSL "$base_url/org/ow2/asm/asm-util/7.3.1/asm-util-7.3.1.jar" -o "/app/repository/plugins/asm-util-7.3.1.jar"; \
	curl -fsSL "$base_url/org/ow2/asm/asm-analysis/7.3.1/asm-analysis-7.3.1.jar" -o "/app/repository/plugins/asm-analysis-7.3.1.jar"
RUN set -eux; \
	for jar_file in /app/repository/plugins/*.jar; do \
		work_dir="$(mktemp -d)"; \
		(cd "$work_dir" && jar xf "$jar_file"); \
		rm -f "$work_dir"/META-INF/*.SF "$work_dir"/META-INF/*.RSA "$work_dir"/META-INF/*.DSA; \
		if [ -f "$work_dir/META-INF/MANIFEST.MF" ]; then \
			(cd "$work_dir" && jar cfm "$jar_file.new" META-INF/MANIFEST.MF .); \
		else \
			(cd "$work_dir" && jar cf "$jar_file.new" .); \
		fi; \
		mv "$jar_file.new" "$jar_file"; \
		rm -rf "$work_dir"; \
	done

FROM eclipse-temurin:21-jdk
WORKDIR /work
EXPOSE 8080
VOLUME ["/work", "/out"]
COPY --from=build /app/repository /app/repository
ENTRYPOINT ["java", "-cp", "/app/repository/plugins/*", "at.ac.tuwien.big.momot.runner.RestServerMain"]
