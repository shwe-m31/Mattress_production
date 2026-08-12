import React, { useState } from "react";
import ReactDOM from "react-dom/client";
import { User, Mail, Lock, Key, Shield } from "lucide-react";
import App from "./App";

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

const styleSheet = document.createElement("style");
styleSheet.textContent = `
  * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Segoe UI', sans-serif; }
  body, html { height: 100%; width: 100%; overflow-y: auto; }

  /* ===== Carousel Styles ===== */
  .carousel-item { min-height: 100vh; width: 100vw; }
  .carousel-item iframe, .carousel-item .slide-container { height: 100%; width: 100%; }
  .slide-container { display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #eaf3ff, #cfdff9); color: #1c2f45; padding: 40px; }
  .left { flex: 1; display: flex; justify-content: center; align-items: center; padding: 2rem; }
  .left img { width: 400px; height: 400px; object-fit: cover; border-radius: 16px; box-shadow: 0 8px 20px rgba(100, 140, 190, 0.2); }
  .right { flex: 1; padding: 3rem 2rem; display: flex; flex-direction: column; justify-content: center; }
  .brand { font-size: 2.5rem; font-weight: bold; background: linear-gradient(135deg, #a4c4f4, #7faee8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 1rem; }
  h2 { font-size: 1.6rem; margin-bottom: 1rem; color: #2e4b6d; }
  p { font-size: 1rem; color: #3e5b7d; margin-bottom: 2rem; line-height: 1.5; }
  .next-btn { background: linear-gradient(135deg, #bcd5fa, #8fbdf5); color: #ffffff; border: none; border-radius: 10px; padding: 14px 32px; font-weight: bold; cursor: pointer; transition: all 0.3s; font-size: 16px; box-shadow: 0 4px 15px rgba(110, 150, 210, 0.3); align-self: flex-start; }
  .next-btn:hover { background: linear-gradient(135deg, #8fbdf5, #bcd5fa); transform: scale(1.05); box-shadow: 0 6px 20px rgba(70, 110, 180, 0.4); }
  .carousel-control-prev, .carousel-control-next { width: 8%; opacity: 1; }
  .carousel-control-prev-icon, .carousel-control-next-icon { background-color: rgba(46, 75, 109, 0.72); border-radius: 50%; background-size: 55%; width: 42px; height: 42px; }

  /* ===== AuthPage Styles ===== */
  .auth-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e8f1ff, #d2e2fb); padding: 20px; }
  .auth-background { width: 100%; max-width: 440px; }
  .auth-card { background: #ffffff; border-radius: 24px; padding: 40px; box-shadow: 0 10px 30px rgba(100, 140, 190, 0.15); border: 1px solid #e3ecfa; animation: fadeInUp 0.6s ease-out; }
  @keyframes fadeInUp { from {opacity: 0; transform: translateY(30px);} to {opacity: 1; transform: translateY(0);} }
  .auth-header { text-align: center; margin-bottom: 32px; }
  .auth-icon { width: 80px; height: 80px; background: linear-gradient(135deg, #a4c4f4, #7faee8); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; }
  .auth-title { font-size: 32px; font-weight: 700; color: #1f3b5c; margin-bottom: 8px; }
  .auth-subtitle { color: #5f8fcf; font-size: 16px; }
  .toggle-background { display: flex; background: #f0f6ff; border-radius: 50px; padding: 6px; position: relative; margin-bottom: 32px; transition: background 0.3s ease; }
  .toggle-background::before { content: ''; position: absolute; top: 6px; left: 6px; width: calc(50% - 6px); height: calc(100% - 12px); background: linear-gradient(135deg, #a4c4f4, #7faee8); border-radius: 50px; box-shadow: 0 4px 12px rgba(120, 160, 210, 0.3); transition: transform 0.3s ease, background 0.3s ease; }
  .toggle-background.login-active::before { transform: translateX(0); }
  .toggle-background.signup-active::before { transform: translateX(100%); }
  .toggle-btn { flex: 1; border: none; background: transparent; border-radius: 50px; font-weight: 600; font-size: 15px; color: #2c4b70; cursor: pointer; z-index: 1; transition: color 0.3s, transform 0.2s; padding: 12px; }
  .toggle-btn.active { color: #ffffff; text-shadow: 0 1px 3px rgba(80, 120, 180, 0.4); transform: scale(1.03); }
  .auth-form { display: flex; flex-direction: column; gap: 16px; }
  .input-group { position: relative; display: flex; align-items: center; }
  .input-icon { position: absolute; left: 16px; color: #7faee8; }
  .auth-input { width: 100%; padding: 16px 16px 16px 48px; border: 2px solid #e0e9fb; border-radius: 12px; font-size: 16px; transition: all 0.3s ease; background: #f7faff; }
  .auth-input:focus { outline: none; background: #ffffff; box-shadow: 0 0 0 3px rgba(160, 190, 240, 0.3); border-color: #a4c4f4; }
  .forgot-password { text-align: right; margin-bottom: 8px; }
  .forgot-link { color: #7faee8; text-decoration: none; font-weight: 600; font-size: 14px; background: none; border: none; padding: 0; cursor: pointer; }
  .submit-btn { background: linear-gradient(135deg, #a4c4f4, #7faee8); color: #ffffff; border: none; padding: 16px; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; transition: 0.3s; box-shadow: 0 6px 18px rgba(110, 150, 210, 0.3); }
  .submit-btn:hover { transform: translateY(-2px); background: linear-gradient(135deg, #7faee8, #a4c4f4); box-shadow: 0 8px 24px rgba(70, 110, 180, 0.35); }
`;
document.head.appendChild(styleSheet);


function IntroCarousel({ onFinish }) {
  return (
    <div id="introCarousel" className="carousel slide" data-bs-ride="carousel" data-bs-interval="2000">
      <div className="carousel-inner">
        <div className="carousel-item active">
          <div className="slide-container">
            <div className="left"><img src="movie.jpg" alt="Spring Mattress" /></div>
            <div className="right">
              <h1 className="brand">PEPS</h1>
              <h2>India’s Leading Spring Mattress Brand</h2>
              <p>Experience luxurious sleep crafted with precision and passion.<br/>Every PEPS mattress is built for your perfect rest.</p>
              <button className="next-btn" type="button" data-bs-target="#introCarousel" data-bs-slide="next">Next</button>
            </div>
          </div>
        </div>
        <div className="carousel-item">
          <div className="slide-container">
            <div className="left"><img src="music.jpg" alt="Hypnos Mattress" /></div>
            <div className="right">
              <h1 className="brand">PEPS HYPNOS</h1>
              <h2>Where Comfort Meets Royalty</h2>
              <button className="next-btn" type="button" data-bs-target="#introCarousel" data-bs-slide="next">Next</button>
              <p>Discover Hypnos by PEPS — designed with European technology to give you hotel-style comfort right at home.</p>
            </div>
          </div>
        </div>
        <div className="carousel-item">
          <div className="slide-container">
            <div className="left"><img src="flight.jpg" alt="Peps Factory" /></div>
            <div className="right">
              <h1 className="brand">PEPS INDUSTRIES</h1>
              <h2>Quality That Never Sleeps</h2>
              <p>From our advanced manufacturing to your bedroom — PEPS ensures unmatched quality, durability, and innovation.</p>
              <button className="next-btn" onClick={onFinish}>Explore PEPS Now!</button>
            </div>
          </div>
        </div>
      </div>
      <button className="carousel-control-prev" type="button" data-bs-target="#introCarousel" data-bs-slide="prev">
        <span className="carousel-control-prev-icon"></span>
      </button>
      <button className="carousel-control-next" type="button" data-bs-target="#introCarousel" data-bs-slide="next">
        <span className="carousel-control-next-icon"></span>
      </button>
    </div>
  );
}


function AuthPage({ onFinish }) {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({ name:"", email:"", password:"", confirmPassword:"" });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isLogin) { alert("Logged in successfully!"); onFinish(); }
    else {
      if (formData.password !== formData.confirmPassword) { alert("Passwords do not match"); return; }
      alert("Account created successfully!"); onFinish();
    }
  };

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  return (
    <div className="auth-container">
      <div className="auth-background">
        <div className="auth-card">
          <div className="auth-header">
            <div className="auth-icon"><Shield size={32} color="white" /></div>
            <h1 className="auth-title">{isLogin ? "Welcome Back" : "Create Account"}</h1>
            <p className="auth-subtitle">{isLogin ? "Sign in to continue" : "Join us today"}</p>
          </div>

          <div className={`toggle-background ${isLogin ? "login-active" : "signup-active"}`}>
            <button className={`toggle-btn ${isLogin ? "active" : ""}`} onClick={() => setIsLogin(true)}>Login</button>
            <button className={`toggle-btn ${!isLogin ? "active" : ""}`} onClick={() => setIsLogin(false)}>Sign Up</button>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {!isLogin && (
              <div className="input-group">
                <User className="input-icon" />
                <input type="text" name="name" placeholder="Full Name" value={formData.name} onChange={handleChange} required className="auth-input" />
              </div>
            )}
            <div className="input-group">
              <Mail className="input-icon" />
              <input type="email" name="email" placeholder="Email Address" value={formData.email} onChange={handleChange} required className="auth-input" />
            </div>
            <div className="input-group">
              <Lock className="input-icon" />
              <input type="password" name="password" placeholder="Password" value={formData.password} onChange={handleChange} required className="auth-input" />
            </div>
            {!isLogin && (
              <div className="input-group">
                <Key className="input-icon" />
                <input type="password" name="confirmPassword" placeholder="Confirm Password" value={formData.confirmPassword} onChange={handleChange} required className="auth-input" />
              </div>
            )}
            {isLogin && (
              <div className="forgot-password">
                <button type="button" className="forgot-link" onClick={() => {
                  const email = prompt("Enter your email to reset password:");
                  if (email) alert(`Password reset link sent to ${email}`);
                }}>Forgot Password?</button>
              </div>
            )}
            <button type="submit" className="submit-btn">{isLogin ? "Sign In" : "Ready, Set, Go!"}</button>
          </form>
        </div>
      </div>
    </div>
  );
}


function Apps() {
  const [step, setStep] = useState(0);
  return (
    <>
      {step === 0 && <IntroCarousel onFinish={() => setStep(1)} />}
      {step === 1 && <AuthPage onFinish={() => setStep(2)} />}
      {step === 2 && <App />}
    </>
  );
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(<Apps />);
