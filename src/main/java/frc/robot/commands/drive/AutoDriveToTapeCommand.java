// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.drive;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.RobotContainer;
import frc.robot.Constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.util.LimelightHelpers;

public class AutoDriveToTapeCommand extends CommandBase {
  private DriveSubsystem driveSubsystem;

  private double yaw = 0;
  private double area = 0;

  private PIDController forwardController = new PIDController(0.95, 0, 0);
  private PIDController turnController = new PIDController(0.0050, 0, 0);

  private int timeAligned = 0;

  /** Creates a new AutoDriveToTape. */
  public AutoDriveToTapeCommand(DriveSubsystem driveSubsystem) {
    this.driveSubsystem = driveSubsystem;

    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(driveSubsystem);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    timeAligned = 0;
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    // PhotonPipelineResult result = camera.getLatestResult();

    if (LimelightHelpers.getTV(DriveConstants.limelightName)) {
      // PhotonTrackedTarget target = result.getBestTarget();

      yaw = LimelightHelpers.getTX(DriveConstants.limelightName);
      area = LimelightHelpers.getTA(DriveConstants.limelightName);

      double turnSpeed = -turnController.calculate(yaw, 0);
      double forwardSpeed = forwardController.calculate(area, DriveConstants.tapeAlignmentArea);

      driveSubsystem.arcadeDrive(MathUtil.clamp(forwardSpeed, -0.15, 0.15), MathUtil.clamp(turnSpeed, -0.15, 0.15));
    } else if (Math.abs(yaw) >= 5
        || Math.abs(area - DriveConstants.tapeAlignmentArea) >= DriveConstants.tapeAlignmentAreaTolerance) {

      double turnSpeed = -turnController.calculate(yaw, 0);
      double forwardSpeed = forwardController.calculate(area, DriveConstants.tapeAlignmentArea);

      driveSubsystem.arcadeDrive(MathUtil.clamp(forwardSpeed, -0.15, 0.15), turnSpeed);
    } else {
      driveSubsystem.arcadeDrive(0, 0);
    }

    // Vibrate the controller if the robot is aligned
    if (driveSubsystem.alignedToTapeYaw() && driveSubsystem.alignedToTapeArea()) {
      RobotContainer.driverControllerHID.setRumble(RumbleType.kRightRumble, 1.00);
      SmartDashboard.putBoolean("Target Aligned", true);
      timeAligned++;
    } else {
      RobotContainer.driverControllerHID.setRumble(RumbleType.kRightRumble, 0);
      SmartDashboard.putBoolean("Target Aligned", false);

      if (timeAligned > 0) {
        timeAligned--;
      }
    }
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    RobotContainer.driverControllerHID.setRumble(RumbleType.kRightRumble, 0);
    SmartDashboard.putBoolean("Target Aligned", false);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return driveSubsystem.alignedToTapeYaw() && driveSubsystem.alignedToTapeArea() && timeAligned >= 5;
  }
}
