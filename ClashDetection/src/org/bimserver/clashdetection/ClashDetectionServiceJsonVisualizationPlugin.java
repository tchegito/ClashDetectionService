package org.bimserver.clashdetection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.store.DoubleType;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

public class ClashDetectionServiceJsonVisualizationPlugin extends AbstractAddExtendedDataService {

	public ClashDetectionServiceJsonVisualizationPlugin() {
		super("http://bimserver.org/3dvisualizationeffects");
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		ClashDetector clashDetector = new ClashDetector(model.getAllWithSubTypes(IfcProduct.class), runningService.getPluginConfiguration().getDouble("margin").floatValue());
		List<Clash> clashes = clashDetector.findClashes();
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		ObjectNode visNode = objectMapper.createObjectNode();
		visNode.put("name", "Clashes");
		ArrayNode changes = objectMapper.createArrayNode();
		visNode.set("changes", changes);
		ObjectNode mainChange = objectMapper.createObjectNode();
		changes.add(mainChange);
		ObjectNode selector = objectMapper.createObjectNode();
		mainChange.set("selector", selector);
		ArrayNode guids = objectMapper.createArrayNode();
		selector.set("guids", guids);
		ObjectNode effect = objectMapper.createObjectNode();
		mainChange.set("effect", effect);
		ObjectNode color = objectMapper.createObjectNode();
		effect.set("color", color);
		color.put("r", 1);
		color.put("g", 0);
		color.put("b", 0);
		color.put("a", 0.7f);
		
		Set<String> guidsSet = new HashSet<>();
		
		for (Clash clash : clashes) {
			guidsSet.add(clash.getIfcProduct1().getGlobalId());
			guidsSet.add(clash.getIfcProduct2().getGlobalId());
		}
		
		for (String guid : guidsSet) {
			guids.add(guid);
		}
		
		System.out.println("Unique GUID's in clashes: " + guids.size());
		
		addExtendedData(visNode.toString().getBytes(Charsets.UTF_8), "visualizationinfo.json", "Clashes (" + guids.size() + ")", "application/json", bimServerClientInterface, roid);
	}
	
	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition objectDefinition = StoreFactory.eINSTANCE.createObjectDefinition();
		ParameterDefinition marginParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		marginParameter.setIdentifier("margin");
		marginParameter.setName("Margin");
		marginParameter.setRequired(true);
		DoubleType defaultValue = StoreFactory.eINSTANCE.createDoubleType();
		defaultValue.setValue(0.1);
		marginParameter.setDefaultValue(defaultValue);
		PrimitiveDefinition doubleDefinition = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		doubleDefinition.setType(PrimitiveEnum.DOUBLE);
		marginParameter.setType(doubleDefinition);
		objectDefinition.getParameters().add(marginParameter);
		return objectDefinition;
	}
}