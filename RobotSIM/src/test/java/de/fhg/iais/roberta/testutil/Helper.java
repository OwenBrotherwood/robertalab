package de.fhg.iais.roberta.testutil;

import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.blockly.generated.Instance;
import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.SimConfiguration;
import de.fhg.iais.roberta.factory.SimFactory;
import de.fhg.iais.roberta.jaxb.JaxbHelper;
import de.fhg.iais.roberta.mode.action.DriveDirection;
import de.fhg.iais.roberta.mode.action.MotorSide;
import de.fhg.iais.roberta.mode.action.sim.ActorPort;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.blocksequence.Location;
import de.fhg.iais.roberta.syntax.codegen.Ast2JavaScriptVisitor;
import de.fhg.iais.roberta.transformer.Jaxb2BlocklyProgramTransformer;
import de.fhg.iais.roberta.transformer.Jaxb2SimConfigurationTransformer;

/**
 * This class is used to store helper methods for operation with JAXB objects and generation code from them.
 */
public class Helper {

    /**
     * Generate java script code as string from a given program .
     *
     * @param pathToProgramXml path to a XML file, usable for {@link Class#getResourceAsStream(String)}
     * @return the code as string
     * @throws Exception
     */
    public static String generateJavaScript(String pathToProgramXml) throws Exception {
        Jaxb2BlocklyProgramTransformer<Void> transformer = generateTransformer(pathToProgramXml);
        Configuration brickConfiguration =
            new SimConfiguration.Builder()
                .addActor(ActorPort.A, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.LEFT))
                .addActor(ActorPort.B, new Actor(ActorType.MEDIUM, true, DriveDirection.FOREWARD, MotorSide.RIGHT))
                .addActor(ActorPort.C, new Actor(ActorType.LARGE, false, DriveDirection.FOREWARD, MotorSide.LEFT))
                .addActor(ActorPort.D, new Actor(ActorType.MEDIUM, false, DriveDirection.FOREWARD, MotorSide.RIGHT))
                .build();
        String code = Ast2JavaScriptVisitor.generate(brickConfiguration, transformer.getTree());
        // System.out.println(code); // only needed for EXTREME debugging
        return code;
    }

    /**
     * return the brick configuration for given XML configuration text.
     *
     * @param blocklyXml the configuration XML as String
     * @return brick configuration
     * @throws Exception
     */
    public static Configuration generateConfiguration(String blocklyXml) throws Exception {
        BlockSet project = JaxbHelper.xml2BlockSet(blocklyXml);
        SimFactory robotModeFactory = new SimFactory(null);
        Jaxb2SimConfigurationTransformer transformer = new Jaxb2SimConfigurationTransformer(robotModeFactory);
        return transformer.transform(project);
    }

    /**
     * return the jaxb transformer for a given program fragment.
     *
     * @param pathToProgramXml path to a XML file, usable for {@link Class#getResourceAsStream(String)}
     * @return jaxb transformer
     * @throws Exception
     */
    public static Jaxb2BlocklyProgramTransformer<Void> generateTransformer(String pathToProgramXml) throws Exception {
        BlockSet project = JaxbHelper.path2BlockSet(pathToProgramXml);
        SimFactory robotModeFactory = new SimFactory(null);
        Jaxb2BlocklyProgramTransformer<Void> transformer = new Jaxb2BlocklyProgramTransformer<>(robotModeFactory);
        transformer.transform(project);
        return transformer;
    }

    /**
     * return the jaxb transformer for a given XML program text.
     *
     * @param blocklyXml the program XML as String
     * @return jaxb the transformer
     * @throws Exception
     */
    public static Jaxb2BlocklyProgramTransformer<Void> generateProgramTransformer(String blocklyXml) throws Exception {
        BlockSet project = JaxbHelper.xml2BlockSet(blocklyXml);
        SimFactory robotModeFactory = new SimFactory(null);
        Jaxb2BlocklyProgramTransformer<Void> transformer = new Jaxb2BlocklyProgramTransformer<>(robotModeFactory);
        transformer.transform(project);
        return transformer;
    }

    /**
     * return the toString representation for a jaxb transformer for a given program fragment.
     *
     * @param pathToProgramXml path to a XML file, usable for {@link Class#getResourceAsStream(String)}
     * @return the toString representation for a jaxb transformer
     * @throws Exception
     */
    public static String generateTransformerString(String pathToProgramXml) throws Exception {
        return generateTransformer(pathToProgramXml).toString();
    }

    /**
     * return the first and only one phrase from a given program fragment.
     *
     * @param pathToProgramXml path to a XML file, usable for {@link Class#getResourceAsStream(String)}
     * @return the first and only one phrase
     * @throws Exception
     */
    public static <V> ArrayList<ArrayList<Phrase<V>>> generateASTs(String pathToProgramXml) throws Exception {
        BlockSet project = JaxbHelper.path2BlockSet(pathToProgramXml);
        SimFactory robotModeFactory = new SimFactory(null);
        Jaxb2BlocklyProgramTransformer<V> transformer = new Jaxb2BlocklyProgramTransformer<>(robotModeFactory);
        transformer.transform(project);
        ArrayList<ArrayList<Phrase<V>>> tree = transformer.getTree();
        return tree;
    }

    /**
     * Generate AST from XML Blockly stored program
     *
     * @param pathToProgramXml
     * @return AST of the program
     * @throws Exception
     */
    public static <V> Phrase<V> generateAST(String pathToProgramXml) throws Exception {
        ArrayList<ArrayList<Phrase<V>>> tree = generateASTs(pathToProgramXml);
        return tree.get(0).get(1);
    }

    /**
     * Asserts if transformation of Blockly XML saved program is correct.<br>
     * <br>
     * <b>Transformation:</b>
     * <ol>
     * <li>XML to JAXB</li>
     * <li>JAXB to AST</li>
     * <li>AST to JAXB</li>
     * <li>JAXB to XML</li>
     * </ol>
     * Return true if the first XML is equal to second XML.
     *
     * @param fileName of the program
     * @throws Exception
     */
    public static void assertTransformationIsOk(String fileName) throws Exception {
        Jaxb2BlocklyProgramTransformer<Void> transformer = generateTransformer(fileName);
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockSet.class);
        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        BlockSet blockSet = astToJaxb(transformer.getTree());
        //        m.marshal(blockSet, System.out); // only needed for EXTREME debugging
        StringWriter writer = new StringWriter();
        m.marshal(blockSet, writer);
        String t = Resources.toString(Helper.class.getResource(fileName), Charsets.UTF_8);
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = XMLUnit.compareXML(writer.toString(), t);

        //        System.out.println(diff.toString()); // only needed for EXTREME debugging
        Assert.assertTrue(diff.identical());
    }

    public static BlockSet astToJaxb(ArrayList<ArrayList<Phrase<Void>>> astProgram) {
        BlockSet blockSet = new BlockSet();

        Instance instance = null;
        for ( ArrayList<Phrase<Void>> tree : astProgram ) {
            for ( Phrase<Void> phrase : tree ) {
                if ( phrase.getKind().hasName("LOCATION") ) {
                    blockSet.getInstance().add(instance);
                    instance = new Instance();
                    instance.setX(((Location<Void>) phrase).getX());
                    instance.setY(((Location<Void>) phrase).getY());
                }
                instance.getBlock().add(phrase.astToBlock());
            }
        }
        blockSet.getInstance().add(instance);
        return blockSet;
    }

    /**
     * Asserts if two XML string are identical by ignoring white space.
     *
     * @param arg1 first XML string
     * @param arg2 second XML string
     * @throws Exception
     */
    public static void assertXML(String arg1, String arg2) throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = XMLUnit.compareXML(arg1, arg2);
        Assert.assertTrue(diff.identical());
    }

    public static String jaxbToXml(BlockSet blockSet) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockSet.class);
        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        m.marshal(blockSet, writer);
        return writer.toString();
    }

}
