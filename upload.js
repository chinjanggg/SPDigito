const multer = require("multer");
const path = require("path");
const crypto = require("crypto");

const storage = multer.diskStorage({
  destination: "./uploads/",
  filename: (req, file, cb) => {
    return crypto.pseudoRandomBytes(8, (err, raw) => {
      if (err) {
        return cb(err);
      }
      return cb(
        null,
        "" + raw.toString("hex") + path.extname(file.originalname)
      );
    });
  }
});

const upload = multer({ storage: storage });

module.exports = upload;
