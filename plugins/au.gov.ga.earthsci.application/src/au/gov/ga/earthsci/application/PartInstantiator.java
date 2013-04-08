package au.gov.ga.earthsci.application;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartInstantiator
{
	private Logger logger = LoggerFactory.getLogger(PartInstantiator.class);

	@Inject
	private IEventBroker broker;

	@Inject
	private EPartService partService;

	private EventHandler handler;

	// Listen to the e4 core service's event broker to find the magical time
	// when the application is created and try to sort the layout.
	@PostConstruct
	void hookListeners(final MApplication application, final EModelService service)
	{
		if (handler == null)
		{
			handler = new EventHandler()
			{
				// Try to sort the layout. Unsubscribe from event broker if
				// successful.
				@Override
				public void handleEvent(Event event)
				{
					try
					{
						createParts(application, service);
						// finished successfully: stop listening to the broker.
						broker.unsubscribe(handler);
					}
					catch (Exception e)
					{
						// Something went wrong, the application model was not ready yet.
						// Keep on listening.
						logger.error("Error creating parts for placeholders", e); //$NON-NLS-1$
					}
				}
			};
			broker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, handler);
		}
	}

	private void createParts(MApplication application, EModelService service)
	{
		// find all placeholders
		List<MPlaceholder> placeholders = service.findElements(application, null, MPlaceholder.class, null);

		for (int i = placeholders.size() - 1; i >= 0; i--)
		{
			MPlaceholder placeholder = placeholders.get(i);
			MPart part = partService.createPart(placeholder.getElementId());
			if (part != null)
			{
				List<MUIElement> siblings = placeholder.getParent().getChildren();
				int index = siblings.indexOf(placeholder);
				siblings.add(index, part);
				siblings.remove(placeholder);
				partService.activate(part);
			}
		}
	}

	@PreDestroy
	void unhookListeners()
	{
		if (handler != null)
		{
			// in case it wasn't unhooked earlier
			broker.unsubscribe(handler);
		}
	}
}