package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private Spatial mundo, area;
    private BulletAppState estadosFisicos;
    private RigidBodyControl fisicaSuelo, fisicaArea, fisicaBolaEnemiga, fisicaBolaJugadora;
    private Geometry bolaEnemiga, bolaJugadora;
    private final AnalogListener analogListener;
    private Vector3f pJugador, pEnemigo;
    float x, y, z, velocidadMaxEnemigo = 1f, velocidadMaxJugador = 2f;
    int saltos = 1;
    boolean gameover = false;
    double angulo, distancia, a, b;
    boolean empujando = false;
    Mesh lineMesh0, lineMeshFinal;
    Geometry lineGeometry1, lineGeometry2;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    public Main() {
        this.analogListener = new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                switch(name) {
                    case "Ahead":
                        fisicaBolaJugadora.clearForces();
                        if(fisicaBolaEnemiga.getLinearVelocity().z > -velocidadMaxJugador) {
                            if(fisicaBolaJugadora.getLinearVelocity().z > 0) {
                                fisicaBolaJugadora.applyCentralForce(new Vector3f(0, 0, -6));
                            }
                            fisicaBolaJugadora.applyCentralForce(new Vector3f(0, 0, -3));
                        }
                        break;
                    case "Behind":
                        fisicaBolaJugadora.clearForces();
                        if(fisicaBolaEnemiga.getLinearVelocity().z < velocidadMaxJugador) {
                            if(fisicaBolaJugadora.getLinearVelocity().z < 0) {
                                fisicaBolaJugadora.applyCentralForce(new Vector3f(0, 0, 6));
                            }
                            fisicaBolaJugadora.applyCentralForce(new Vector3f(0, 0, 3));
                        }
                        break;
                    case "Left":
                        fisicaBolaJugadora.clearForces();
                        if(fisicaBolaEnemiga.getLinearVelocity().x > -velocidadMaxJugador) {
                            if(fisicaBolaJugadora.getLinearVelocity().x > 0) {
                                fisicaBolaJugadora.applyCentralForce(new Vector3f(-6, 0, 0));
                            }
                            fisicaBolaJugadora.applyCentralForce(new Vector3f(-3, 0, 0));
                        }
                        break;
                    case "Right":
                        fisicaBolaJugadora.clearForces();
                        if(fisicaBolaEnemiga.getLinearVelocity().x < velocidadMaxJugador) {
                            if(fisicaBolaJugadora.getLinearVelocity().x < 0) {
                                fisicaBolaJugadora.applyCentralForce(new Vector3f(6, 0, 0));
                            }
                            fisicaBolaJugadora.applyCentralForce(new Vector3f(3, 0, 0));
                        }
                        break;
                    case "Up":
                        if(saltos > 0) {
                            fisicaBolaJugadora.clearForces();
                            fisicaBolaJugadora.setLinearVelocity(new Vector3f(0, 10, 0));
                            saltos--;
                        }
                        break;
                }
            }
        };
    }
    
    //Colisiones
    private final PhysicsCollisionListener physicsCollisionListener = new PhysicsCollisionListener() {
        @Override
        public void collision(PhysicsCollisionEvent event) {
            if (event.getNodeA().getName().equals("bolaJugadora") && event.getNodeB().getName().equals("suelo")) {
                if(!gameover) {
                    //Texto de GameOver
                    System.out.println("Fin");
                    gameover = true;
                }
            }
        }
    };

    @Override
    public void simpleInitApp() {
        //Cámara
        this.flyCam.setEnabled(true);
        
        //Fisica
        estadosFisicos = new BulletAppState();
        stateManager.attach(estadosFisicos);
        
        //Carga mundo
        mundo = assetManager.loadModel("Scenes/escena.j3o");
        mundo.setName("suelo");
        rootNode.attachChild(mundo);
        fisicaSuelo = new RigidBodyControl(0.0f);
        mundo.addControl(fisicaSuelo);
        estadosFisicos.getPhysicsSpace().add(fisicaSuelo);
        fisicaSuelo.setRestitution(0.9f);
        
        //Cielo
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        
        //Ajusta luces
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(0f, -5f, -1f).normalizeLocal());
        light.setColor(ColorRGBA.White);
        rootNode.addLight(light);
        
        //Carga area
        area = assetManager.loadModel("Models/area.j3o");
        area.setName("area");
        rootNode.attachChild(area);
        area.setLocalTranslation(new Vector3f(0, 0, 0));
        fisicaArea = new RigidBodyControl(0.0f);
        area.addControl(fisicaArea);
        estadosFisicos.getPhysicsSpace().add(fisicaArea);
        fisicaArea.setRestitution(0.9f);
        
        //Bola enemiga
        Sphere sphere1 = new Sphere(32, 32, 0.4f);
        TangentBinormalGenerator.generate(sphere1);
        Material mat_bola1 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_bola1.setTexture("DiffuseMap", assetManager.loadTexture("Textures/terrain-alpha/campo-terrain-campo-alphablend0.png"));
        mat_bola1.setBoolean("UseMaterialColors",true);
        mat_bola1.setColor("Diffuse",ColorRGBA.LightGray);
        mat_bola1.setColor("Specular",ColorRGBA.White);
        mat_bola1.setFloat("Shininess", 64f);
        bolaEnemiga = new Geometry("bolaEnemiga", sphere1);
        bolaEnemiga.setMaterial(mat_bola1);
        bolaEnemiga.setLocalTranslation(new Vector3f(0, 4f, -5));
        rootNode.attachChild(bolaEnemiga);
        fisicaBolaEnemiga = new RigidBodyControl(1f);
        bolaEnemiga.addControl(fisicaBolaEnemiga);
        estadosFisicos.getPhysicsSpace().add(fisicaBolaEnemiga);
        fisicaBolaEnemiga.setRestitution(0.6f);
        fisicaBolaEnemiga.setMass(5f);
        
        //Bola jugadora
        Sphere sphere2 = new Sphere(32, 32, 0.4f);
        TangentBinormalGenerator.generate(sphere2);
        Material mat_bola2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_bola2.setTexture("DiffuseMap", assetManager.loadTexture("Textures/dirt.jpg"));
        mat_bola2.setBoolean("UseMaterialColors",true);
        mat_bola2.setColor("Diffuse",ColorRGBA.LightGray);
        mat_bola2.setColor("Specular",ColorRGBA.White);
        mat_bola2.setFloat("Shininess", 64f);
        bolaJugadora = new Geometry("bolaJugadora", sphere2);
        bolaJugadora.setMaterial(mat_bola2);
        bolaJugadora.setLocalTranslation(new Vector3f(0, 4f, 0));
        rootNode.attachChild(bolaJugadora);
        fisicaBolaJugadora = new RigidBodyControl(1f);
        bolaJugadora.addControl(fisicaBolaJugadora);
        estadosFisicos.getPhysicsSpace().add(fisicaBolaJugadora);
        fisicaBolaJugadora.setRestitution(0.6f);
        fisicaBolaJugadora.setMass(0.5f);
        
        //Control por teclado
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ahead", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Behind", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(analogListener, "Left", "Right", "Ahead", "Behind", "Up");
        
        //Colisiones
        estadosFisicos.getPhysicsSpace().addCollisionListener(physicsCollisionListener);
        
        //Linea a 0
        lineMesh0 = new Mesh();
        lineMesh0.setMode(Mesh.Mode.Lines);
        lineGeometry1 = new Geometry("line", lineMesh0);
        Material lineMaterial1 = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        lineGeometry1.setMaterial(lineMaterial1);
        rootNode.attachChild(lineGeometry1);
        
        //Linea a final
        lineMeshFinal = new Mesh();
        lineMeshFinal.setMode(Mesh.Mode.Lines);
        lineGeometry2 = new Geometry("line", lineMeshFinal);
        Material lineMaterial2 = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        lineGeometry2.setMaterial(lineMaterial2);
        rootNode.attachChild(lineGeometry2);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        pJugador = fisicaBolaJugadora.getPhysicsLocation();
        cam.setLocation(new Vector3f(pJugador.x, pJugador.y+4f, pJugador.z+9f));
        cam.lookAt(new Vector3f(pJugador.x, pJugador.y+2f, pJugador.z-3f), Vector3f.UNIT_Y);
        
        pEnemigo = fisicaBolaEnemiga.getPhysicsLocation();
        pJugador = bolaJugadora.getLocalTranslation();
        
        //Calcula dirección
        angulo  = Math.atan2(pJugador.z, pJugador.x);
        distancia = Math.sqrt(pJugador.x*pJugador.x + pJugador.z*pJugador.z) + 1.2;
        a = distancia * Math.cos(angulo);
        b = distancia * Math.sin(angulo);
        
        //Actualiza linea a 0
        if(empujando) {
            lineGeometry1.setMaterial(assetManager.loadMaterial("Common/Materials/WhiteColor.j3m"));
        } else {
            lineGeometry1.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        }
        lineMesh0.setBuffer(VertexBuffer.Type.Position, 3, new float[]{pEnemigo.x, pEnemigo.y, pEnemigo.z, 0, pEnemigo.y, 0});
        lineMesh0.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1});
        lineMesh0.updateBound();
        lineMesh0.updateCounts();
        
        //Actualiza linea a final
        if(!empujando) {
            lineGeometry2.setMaterial(assetManager.loadMaterial("Common/Materials/WhiteColor.j3m"));
        } else {
            lineGeometry2.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        }
        lineMeshFinal.setBuffer(VertexBuffer.Type.Position, 3, new float[]{pEnemigo.x, pEnemigo.y, pEnemigo.z, (float)a, pEnemigo.y, (float)b});
        lineMeshFinal.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1});
        lineMeshFinal.updateBound();
        lineMeshFinal.updateCounts();
        
        //if(empujando || posicionarse(new Vector3f((float)a, pJugador.y, (float)b))) {
        //    empujando = !posicionarse(new Vector3f(0, 0, 0));
        //    
        //}
    }
    
    public boolean posicionarse(Vector3f pFinal) {
        fisicaBolaEnemiga.clearForces();
        
        boolean buenaPosicionX = Math.abs(Math.abs(pEnemigo.x) - Math.abs(pFinal.x)) < 0.05f;
        boolean buenaPosicionZ = Math.abs(Math.abs(pEnemigo.z) - Math.abs(pFinal.z)) < 0.05f;
        if(buenaPosicionX && buenaPosicionZ) {
            return true;
        }
        
        int factor = empujando ? 2 : 1;
        
        //Mueve en eje X
        if(pEnemigo.x < pFinal.x) {
            if(fisicaBolaEnemiga.getLinearVelocity().x < 0 ) {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(20*factor, 0, 0));
            } else {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(5*factor, 0, 0));
            }
        } else {
            if(fisicaBolaEnemiga.getLinearVelocity().x > 0) {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(-20*factor, 0, 0));
            } else {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(-5*factor, 0, 0));
            }
        }
        
        //Mueve en eje Z
        if(pEnemigo.z < pFinal.z) {
            if(fisicaBolaEnemiga.getLinearVelocity().z < 0) {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(0, 0, 20*factor));
            } else {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(0, 0, 5*factor));
            }
        } else {
            if(fisicaBolaEnemiga.getLinearVelocity().z > 0) {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(0, 0, -20*factor));
            } else {
                fisicaBolaEnemiga.applyCentralForce(new Vector3f(0, 0, -5*factor));
            }
        }
        
        return false;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
