package v.ast;

public class vColor {

    public static final Integer[] NEGRO    = {0, 0, 0};
    public static final Integer[] BLANCO   = {255, 255, 255};
    public static final Integer[] ROJO     = {255, 0, 0};
    public static final Integer[] VERDE    = {0, 255, 0};
    public static final Integer[] AZUL     = {0, 0, 255};
    public static final Integer[] AMARILLO = {255, 255, 0};
    public static final Integer[] CYAN     = {0, 255, 255};
    public static final Integer[] MAGENTA  = {255, 0, 255};
    public static final Integer[] MARRON   = {139, 69, 19};


    private Integer[] rgb = new Integer[3];

    public vColor(){
        this.rgb = vColor.NEGRO;
    }

    public vColor(String name){
        switch (name) {
            case "negro":
                this.rgb = vColor.NEGRO;
                break;
            case "blanco":
                this.rgb = vColor.BLANCO;
                break;
            case "rojo":
                this.rgb = vColor.ROJO;
                break;
            case "verde":
                this.rgb = vColor.VERDE;
                break;
            case "azul":
                this.rgb = vColor.AZUL;
                break;
            case "amarillo":
                this.rgb = vColor.AMARILLO;
                break;
            case "cyan":
                this.rgb = vColor.CYAN;
                break;
            case "magenta":
                this.rgb = vColor.MAGENTA;
                break;
            case "marron":
                this.rgb = vColor.MARRON;
                break;
            default:
                throw new IllegalArgumentException("Color no reconocido: " + name);
        }
    }

    public vColor(Integer r, Integer g, Integer b) {
        this.rgb[0] = r;
        this.rgb[1] = g;
        this.rgb[2] = b;
    }

    public Integer[] getValue(){
        return this.rgb;
    }
}
