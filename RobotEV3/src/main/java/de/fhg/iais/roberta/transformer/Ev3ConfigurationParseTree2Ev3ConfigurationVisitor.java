package de.fhg.iais.roberta.transformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.EV3Configuration;
import de.fhg.iais.roberta.components.Sensor;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationBaseVisitor;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationLexer;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.ActorContext;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.AdeclContext;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.ConfContext;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.MotorSpecContext;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.SdeclContext;
import de.fhg.iais.roberta.ev3Configuration.generated.Ev3ConfigurationParser.SizesContext;
import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.inter.mode.action.IActorPort;
import de.fhg.iais.roberta.inter.mode.action.IDriveDirection;
import de.fhg.iais.roberta.inter.mode.sensor.ISensorPort;
import de.fhg.iais.roberta.mode.action.MotorSide;
import de.fhg.iais.roberta.util.Formatter;
import de.fhg.iais.roberta.util.Option;

public class Ev3ConfigurationParseTree2Ev3ConfigurationVisitor extends Ev3ConfigurationBaseVisitor<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(Ev3ConfigurationParseTree2Ev3ConfigurationVisitor.class);

    private final Configuration.Builder builder = new EV3Configuration.Builder();
    private IRobotFactory factory;
    private IActorPort nextActorToAttach = null;
    private String parseErrorMessage = null;

    /**
     * take a brick configuration program as String, parse it, create a visitor as an instance of this class and visit the parse tree to create a configuration.
     * <br>
     * Factory method
     */
    public static Option<Configuration> startWalkForVisiting(String stmt, IRobotFactory factory) {
        Ev3ConfigurationParseTree2Ev3ConfigurationVisitor visitor = new Ev3ConfigurationParseTree2Ev3ConfigurationVisitor(factory);
        visitor.parseAndVisit(stmt);
        return visitor.result();
    }

    private Option<Configuration> result() {
        if ( this.parseErrorMessage == null ) {
            return Option.of(this.builder.build());
        } else {
            return Option.empty(this.parseErrorMessage);
        }
    }

    private Ev3ConfigurationParseTree2Ev3ConfigurationVisitor(IRobotFactory factory) {
        this.factory = factory;
        // intentionally
    }

    private void parseAndVisit(String stmt) {
        try {
            InputStream inputStream = new ByteArrayInputStream(stmt.getBytes("UTF-8"));
            ANTLRInputStream input = new ANTLRInputStream(inputStream);
            Ev3ConfigurationLexer lex = new Ev3ConfigurationLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            Ev3ConfigurationParser parser = new Ev3ConfigurationParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol,
                    int line,
                    int charPositionInLine,
                    String msg,
                    RecognitionException e) {
                    if ( Ev3ConfigurationParseTree2Ev3ConfigurationVisitor.this.parseErrorMessage == null ) {
                        Ev3ConfigurationParseTree2Ev3ConfigurationVisitor.this.parseErrorMessage =
                            "syntax error at line " + line + ":" + charPositionInLine + " --- " + msg;
                    }
                }
            });
            ConfContext tree = parser.conf();
            if ( this.parseErrorMessage == null ) {
                visit(tree);
            }
        } catch ( Exception e ) {
            LOG.error("exception when parsing configuration", e);
            this.parseErrorMessage = "exception when parsing configuration - should not happen. Contact the developers.";
        }
    }

    @Override
    public Void visitSizes(SizesContext ctx) {
        try {
            this.builder.setWheelDiameter(Formatter.s2d(ctx.RATIONAL(0).getText()));
            this.builder.setTrackWidth(Formatter.s2d(ctx.RATIONAL(1).getText()));
        } catch ( ParseException e ) {
            throw new RuntimeException("Keys.E1", e);
        }
        return null;
    }

    @Override
    public Void visitSdecl(SdeclContext ctx) {
        ISensorPort port = this.factory.getSensorPort(ctx.SENSORPORT().getText());
        String sensorShortName = ctx.SENSOR().getText();
        SensorType attachedSensor = null;

        for ( SensorType s : SensorType.values() ) {
            if ( s.toString().equalsIgnoreCase(sensorShortName) ) {
                attachedSensor = s;
                break;
            }
        }
        if ( attachedSensor == null ) {
            throw new RuntimeException("Keys.E2");
        }
        this.builder.addSensor(port, new Sensor(attachedSensor));
        return null;
    }

    @Override
    public Void visitAdecl(AdeclContext ctx) {
        this.nextActorToAttach = this.factory.getActorPort(ctx.ACTORPORT().getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitActor(ActorContext ctx) {
        ActorType motorKind;
        if ( "large".equalsIgnoreCase(ctx.MOTORKIND().getText()) ) {
            motorKind = ActorType.LARGE;
        } else {
            motorKind = ActorType.MEDIUM;
        }
        MotorSpecContext motorSpec = ctx.motorSpec();
        boolean regulated = "regulated".equalsIgnoreCase(motorSpec.REGULATION().getText());
        IDriveDirection direction = this.factory.getDriveDirection(motorSpec.ROTATION().getText());
        //        if ( "forward".equalsIgnoreCase(motorSpec.ROTATION().getText()) ) {
        //            direction = this.factory.getDriveDirection("forward");
        //        } else {
        //            direction = this.factory.getDriveDirection("backward");
        //        }
        TerminalNode leftOrRight = motorSpec.LEFTORRIGHT();
        MotorSide motorSide;
        if ( leftOrRight == null ) {
            motorSide = MotorSide.NONE;
        } else if ( "left".equalsIgnoreCase(leftOrRight.getText()) ) {
            motorSide = MotorSide.LEFT;
        } else {
            motorSide = MotorSide.RIGHT;
        }
        Actor actor = new Actor(motorKind, regulated, direction, motorSide);
        this.builder.addActor(this.nextActorToAttach, actor);
        this.nextActorToAttach = null;
        return null;
    }

}