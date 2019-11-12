const multer = require("multer");
const path = require("path");
const crypto = require("crypto");
const mkdirp = require("mkdirp");

var filename;

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    return crypto.pseudoRandomBytes(8, (err, raw) => {
      if (err) {
        return err;
      }
      filename = raw.toString("hex");
      filepath = "./uploads/" + filename + "/";
      mkdirp(filepath, err => {
        if (err) {
          return cb(err);
        } else {
          //Folder successfully created
          return cb(null, filepath);
        }
      });
    });
  },
  filename: (req, file, cb) => {
    return cb(null, "" + filename + path.extname(file.originalname));
  }
});

const upload = multer({ storage: storage });

module.exports = upload;
