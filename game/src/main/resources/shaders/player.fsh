
in vec2 uv;

uniform float millis;

out vec4 out_Color;

void main () {
    float tip = 1.0 - uv.y;
    out_Color = vec4(tip * 1.6, 0.0, 0.0, tip * 1.4);
}