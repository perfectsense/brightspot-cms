define({
  
  generateFromHue: function(hue) {
    return 'hsl(' + (hue * 360) + ', 70.1%, 54.1%)';
  },
  
  changeHue: function(hue) {
    if (!hue) {
      return 0.5396341463414634;
    }

    var GOLDEN_RATIO = 0.618033988749895;
    hue += GOLDEN_RATIO;
    hue %= 1.0;
    return hue;  
  },
  
  /**
   * Calculates the hue value for a given hex code.
   */
  getHue: function(hex) {
    var r = parseInt(hex.substr(1,2), 16);
    var g = parseInt(hex.substr(3,2), 16);
    var b = parseInt(hex.substr(5,2), 16);
    
    return this.rgbToHsl(r, g, b)[0];
  },
  
  // https://github.com/mjackson/mjijackson.github.com/blob/master/2008/02/rgb-to-hsl-and-rgb-to-hsv-color-model-conversion-algorithms-in-javascript.txt
  rgbToHsl: function(r, g, b) {
    r /= 255, g /= 255, b /= 255;
    var max = Math.max(r, g, b), min = Math.min(r, g, b);
    var h, s, l = (max + min) / 2;

    if(max == min){
        h = s = 0; // achromatic
    }else{
        var d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch(max){
            case r: h = (g - b) / d + (g < b ? 6 : 0); break;
            case g: h = (b - r) / d + 2; break;
            case b: h = (r - g) / d + 4; break;
        }
        h /= 6;
    }

    return [h, s, l]; 
  }
});