import os
import datetime
import hashlib
from flask import Flask, session, url_for, redirect, render_template, request, abort, flash
from werkzeug.utils import secure_filename
import requests


app = Flask(__name__)
app.config.from_object('config')

@app.errorhandler(401)
def FUN_401(error):
    return render_template("page_401.html"), 401

@app.errorhandler(403)
def FUN_403(error):
    return render_template("page_403.html"), 403

@app.errorhandler(404)
def FUN_404(error):
    return render_template("page_404.html"), 404

@app.errorhandler(405)
def FUN_405(error):
    return render_template("page_405.html"), 405

@app.errorhandler(413)
def FUN_413(error):
    return render_template("page_413.html"), 413




#-----------------------------------------------------------------------------
# External communication with MServer


#-----------------------------------------------------------------------------

@app.route("/")
def FUN_root():
    return render_template("index.html")


@app.route("/private/")
def FUN_private():
    if "current_user" in session.keys():
        surl_fuel = app.config["SERVER_URL"] + "fuel-level"
        surl_tires = app.config["SERVER_URL"] + "tire-pressure"
        surl_temp = app.config["SERVER_URL"] + "ac-control"
        headers = {'content-type': 'application/json', 
                   'Authorization': session["token"]}
        
        r_fuel = requests.get(surl_fuel, headers=headers)
        r_tirep = requests.get(surl_tires, headers=headers)
        r_temp = requests.get(surl_temp, headers=headers)
        
        fuel_level = r_fuel.json()["fuel-level"]
        
        front_right = r_tirep.json()["rf"] 
        front_left = r_tirep.json()["lf"] 
        rear_left = r_tirep.json()["lb"] 
        rear_right = r_tirep.json()["rb"] 
        
        ac_temp = r_temp.json()["temperature"]
        
        return render_template("private_page.html", 
                              fuel_level = int(fuel_level),
                               front_left = front_left,
                               front_right = front_right,
                               rear_left = rear_left,
                               rear_right = rear_right,
                               ac_temp = ac_temp)
    else:
        return abort(401)


@app.route("/login", methods = ["POST"])
def FUN_login():
    id_submitted = request.form.get("id").upper()
    server_url = app.config["SERVER_URL"] + "login"
    data = {"username": request.form.get("id"),
            "password": request.form.get("pw")}
    r = requests.post(server_url, data)
    res_json = r.json()
    if r.status_code == 200: 
        session['current_user'] = res_json["user"]
        session['token'] = r.headers["Authorization"]

    return(redirect(url_for("FUN_root")))

@app.route("/logout/")
def FUN_logout():
    session.pop("current_user", None)
    session.pop("token", None)
    return(redirect(url_for("FUN_root")))

@app.route("/set_temp", methods = ["POST"])
def FUN_set_temp():
    text_to_write = request.form.get("temperature")
    surl_temp = app.config["SERVER_URL"] + "ac-control"
    headers = {'Authorization': session["token"]}
    data = {"temperature": text_to_write} 
    r = requests.post(surl_temp, data, headers=headers)
    return(redirect(url_for("FUN_private")))

@app.route("/set_doors", methods = ["POST"])
def FUN_set_doors():
    text = request.form.get("a")
    surl_o_door = app.config["SERVER_URL"] + "open-door"
    surl_c_door = app.config["SERVER_URL"] + "close-door"
    headers = {'content-type': 'application/json', 
                'Authorization': session["token"]}
    
    if text == "Open":
        r = requests.get(surl_o_door, headers=headers)
    else:
        r = requests.get(surl_c_door, headers=headers)
    
    return(redirect(url_for("FUN_private")))

@app.route("/set_binary", methods = ["POST"])
def FUN_set_binary():
    surl_binary = app.config["SERVER_URL"] + "binary"
    headers = {'content-type': 'application/json', 
                'Authorization': session["token"]}
    
    r = requests.post(surl_binary, headers=headers)
    
    return(redirect(url_for("FUN_private")))


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0")
