package fabricish;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.filter.RegexFilter;

final class LogNoise {
	private LogNoise() {
	}

	static void install() {
		Configurator.setLevel(
			"net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate", Level.ERROR);

		try {
			LoggerContext context = (LoggerContext) LogManager.getContext(false);
			Filter filter = RegexFilter.createFilter(
				"(?s).*(without the packet context present|Please report this to mods in the stack-trace below).*",
				null, true, Filter.Result.DENY, Filter.Result.NEUTRAL);
			context.getConfiguration().getRootLogger().addFilter(filter);
			context.updateLoggers();
		} catch (RuntimeException | ReflectiveOperationException e) {
			FabricIshMod.LOGGER.warn("Could not install the log filter: {}", e.toString());
		}
	}
}
