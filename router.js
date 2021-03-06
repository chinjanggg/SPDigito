const express = require("express");
const router = express.Router();
const upload = require("./upload");
const pdata = require("./data/patient-data.json");
const path = require("path");

let { PythonShell } = require("python-shell");

router.get("/", (req, res) => {
  res.render("index", { title: "SP Digito" });
});

router.get("/error", (req, res) => {
  res.render("error", { title: "Error" });
});

//Get patient data for android
router.get("/patientdata", (req, res) => {
  var pid = req.query.pid;
  var pinfo = get_pdata(pid);
  console.log(pinfo);
  res.statusMessage = JSON.stringify(pinfo);
  res.status(200).end();
});

//----------------Route for Manual----------------//
router.get("/manualform", (req, res) => {
  var result = { sys: null, dia: null, pulse: null };
  res.render("manualform", { title: "Manual Form", result: result });
});

router.post("/postresult", (req, res) => {
  let sess = req.session;
  sess.pid = req.body.pid.toUpperCase();
  var result = { sys: 0, dia: 0, pulse: 0 };
  result.sys = req.body.sys;
  result.dia = req.body.dia;
  result.pulse = req.body.pulse;
  sess.result = result;
  res.redirect("/mconfirmation");
});

//Confirm
router.get("/mconfirmation", (req, res) => {
  let sess = req.session;
  if (sess.pid) {
    var pid = sess.pid;
    var result = sess.result;
    var pinfo = get_pdata(pid);

    res.render("mconfirmation", {
      title: "Confirmation",
      pid: pid,
      pinfo: pinfo,
      result: result
    });
  } else {
    res.redirect("/error");
  }
});

//Edit form
router.get("/medit", (req, res) => {
  let sess = req.session;
  if (sess.pid) {
    var pid = sess.pid;
    var result = sess.result;
    var pinfo = get_pdata(pid);
    res.render("medit", {
      title: "Edit",
      pid: pid,
      pinfo: pinfo,
      result: result
    });
  } else {
    res.redirect("/error");
  }
});

//----------------Route for OCR----------------//
router.get("/imageupload", (req, res) => {
  res.render("imageupload", { title: "Image Upload" });
});

// Upload image
router.post("/upload", upload.single("upload"), (req, res) => {
  let sess = req.session;
  var pid = req.body.pid.toUpperCase();
  var img = req.file.filename;
  sess.pid = pid;
  sess.img = img;
  console.log("pid:", sess.pid);
  console.log("File", req.file);
  res.redirect("/ocr/" + pid + "/" + img);
  //res.redirect("/uploads/" + req.file.filename);
  return res.status(200);
});

router.get("/ocr/:pid/:img", digit_ocr);

//Confirm
router.get("/confirmation", (req, res) => {
  let sess = req.session;
  if (sess.pid) {
    var img = sess.img;
    var pid = sess.pid;
    var result = sess.result;
    var pinfo = get_pdata(pid);
    var imgname = path.parse(img).name;
    var imgpath = path.join("/uploads", imgname, img);

    res.render("confirmation", {
      title: "Confirmation",
      imagefile: imgpath,
      pid: pid,
      pinfo: pinfo,
      result: result
    });
  } else {
    res.redirect("/error");
  }
});

//OCR edit page
router.get("/edit", (req, res) => {
  let sess = req.session;
  if (sess.pid) {
    var img = sess.img;
    var pid = sess.pid;
    var result = sess.result;
    var pinfo = get_pdata(pid);
    var imgname = path.parse(img).name;
    var imgpath = path.join("/uploads", imgname, img);

    res.render("edit", {
      title: "Edit",
      imagefile: imgpath,
      pid: pid,
      pinfo: pinfo,
      result: result
    });
  } else {
    res.redirect("/error");
  }
});

//Recheck
router.post("/confirm", (req, res) => {
  let sess = req.session;
  sess.pid = req.body.pid.toUpperCase();
  sess.result = { sys: null, dia: null, pulse: null };
  sess.result.sys = req.body.sys;
  sess.result.dia = req.body.dia;
  sess.result.pulse = req.body.pulse;

  res.redirect("/confirmation");
});

//Save Result
router.post("/savedata", save_data);

router.get("/contact", (req, res) => {
  res.render("contact", { title: "Contact" });
});

//----------------- function -----------------//
//Get patient data
function get_pdata(pid) {
  var pinfo;
  for (patient in pdata) {
    if (pdata[patient].pid == pid) {
      pinfo = pdata[patient];
    }
  }
  return pinfo;
}
//OCR script
function digit_ocr(req, res) {
  let sess = req.session;
  var img = req.params.img;
  var msg, result;

  let options = {
    mode: "text",
    pythonPath: process.env.PYTHONPATH,
    pythonOptions: ["-u"], // get print results in real-time
    scriptPath: "./python",
    args: [img]
  };

  let pyshell = new PythonShell("digit_recognize.py", options);
  pyshell.send(img);

  pyshell.on("message", message => {
    console.log(message);
    msg = message;
  });

  pyshell.end((err, code, signal) => {
    if (err) {
      console.log(err);
    }

    //Edit string json result from python
    result = msg.replace(/'/g, '"');
    res.statusMessage = result;

    result = JSON.parse(result);

    if (req.session.pid) {
      //For Web
      sess.result = result;
      res.redirect("/confirmation");
    } else {
      //There's no session, For Android Application
      res.status(200).end();
    }
  });
}

//Save script
function save_data(req, res) {
  console.log(req.body);
  var data = { pid: "", sys: 0, dia: 0, pulse: 0 };
  data.pid = req.body.pid;
  data.sys = req.body.sys;
  data.dia = req.body.dia;
  data.pulse = req.body.pulse;

  let options = {
    mode: "text",
    pythonPath: process.env.PYTHONPATH,
    pythonOptions: ["-u"], // get print results in real-time
    scriptPath: "./python",
    args: [JSON.stringify(data)]
  };

  let pyshell = new PythonShell("save_output.py", options);
  pyshell.send(JSON.stringify(data));

  pyshell.on("message", message => {
    console.log(message);
  });

  pyshell.end((err, code, signal) => {
    if (err) {
      console.log(err);
      res.redirect("/error");
    } else {
      //Clear session
      req.session.destroy(err => {
        if (err) {
          return console.log(err);
        }
      });
      //Then redirect to index
      res.redirect("/");
      return res.status(200).end();
    }
  });
}

module.exports = router;
