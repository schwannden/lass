function pmToColor(pm) {
  if (pm < 51) return "rgb(1, 228, 0)"
  else if (pm < 101) return "rgb(255, 255, 0)"
  else if (pm < 151) return "rgb(255, 126, 0)"
  else if (pm < 201) return "rgb(254, 0, 0)"
  else if (pm < 301) return "rgb(152, 0, 75)"
  else return "rgb(126, 1, 35)"
}

document.addEventListener("DOMContentLoaded", function() {
    var pm = 1 * document.getElementById("pm").innerText;
    var pmPredict = 1 * document.getElementById("pmPredict").innerText;
    var pmRatio = (pm / (pm + pmPredict)) * 100;

    var pmElem = document.getElementsByClassName("pm")[0];
    var pmPredElem = document.getElementsByClassName("pmPredict")[0];
    pmElem.style.height = pmRatio + "%";
    pmElem.style.background = pmToColor(pm);
    pmPredElem.style.height = (100-pmRatio) + "%";
    pmPredElem.style.background = pmToColor(pmPredict);

    var bars = document.getElementsByClassName('bar');
    var pmScale = document.getElementById('pmScale');
    for (var i = 0; i < bars.length; i++) {
      bars[i].addEventListener('click', function () {
        pmScale.classList.add('focused');
      })
    }

    pmScale.addEventListener('click', function () {
      this.classList.remove('focused');
    });
});
