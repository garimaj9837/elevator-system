package com.lld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*  Functional Requirement:
 *   The System should be able to support N number of Elevators
 *   A user should be able to make a Hall call (up/down)
 *   A user inside the elevator can make a cabin call.
 *   The system supports multiple algorithm for elevator selection.
 *   Each elevator has a direction(Up/down/idle) and states (Moving, stopped,idle)
 *   The system should have elevator controller managing request and assigning elevators.
 *   --Good to have--
 *   capacity overload handling
 *   display board in each lift-- gets updated
 *   Emergency handling
 *   
 *   Non Functional Requirement:
 *    Scalability: Add elevator
 *    Extensibility: 
 *    Thread Safety:
 *      --> Each Elevator is running on it's own thread
 *      --> Multiple users should be able to make calls
 *      --> Maintains the consistency in between the multiple calls
 *     Clean and solid principle adhearing code
 */   

//Elevator
//Request
//ElevatorSystem
//ElevatorState
//DoorState
//ElevatorSelectionStrategy


public class ElevatorSystem {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		ElevatorSelectionStrategy strategy=new NearestElevatorStategy();
		Elevator el1=new Elevator(1,200);
		Elevator el2=new Elevator(2,200);
		List<Elevator> elevators=List.of(el1,el2);
		ElevatorController controller=new ElevatorController(strategy, elevators);
		Building building = new Building(controller,10);
		
		System.out.println("Starting building with " + elevators.size()
        + " elevators, each on its own thread.\n");
		controller.start();
		
		ExecutorService executor=Executors.newFixedThreadPool(4);
		executor.submit(() -> controller.assignElevator(5,Direction.UP));
		executor.submit(() -> controller.assignElevator(6,Direction.DOWN));
		executor.submit(() -> controller.assignElevator(2,Direction.DOWN));
		executor.submit(() -> controller.assignElevator(1,Direction.DOWN));
		
		
		Thread.sleep(6000);
		executor.submit(() -> controller.selectFloor(1, 8));
		executor.submit(() -> controller.selectFloor(2, 0));
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

        // Let elevators actually run for a while (real time, since each has its own thread)
        Thread.sleep(6000);

        controller.shutdown();

        System.out.println("\nFinal elevator positions:");
        for (Elevator elevator : controller.getElevators()) {
            System.out.println("Elevator " + elevator.getElevatorId() + " at floor "
                    + elevator.getCurrentFloor() + ", state=" + elevator.getCurrentState());
        }
	}
	
	

}

enum ElevatorState{
	MOVING,IDLE,STOPPED
}

enum DoorState{
	OPEN,CLOSED
}
enum Direction{
	UP,DOWN,IDLE
}


class Elevator implements Runnable{
	private final int elevator_id;
	private final int tickDurationMillis;
	
	ElevatorState elevatorState;
	DoorState doorState;
	Direction elevatorDirection;
	int currentFloor;
	
	private final TreeSet<Integer> upstops;
	private final TreeSet<Integer> downstops;
	
	boolean running;
	
	
	Elevator(int id, int tickDurationMillis){
		elevator_id=id;
		this.tickDurationMillis=tickDurationMillis;
		elevatorState=ElevatorState.IDLE;
		doorState=DoorState.CLOSED;
		elevatorDirection=Direction.IDLE;
		upstops=new TreeSet<>();
		downstops=new TreeSet<>(Collections.reverseOrder());
		currentFloor=0;
		running=false;
	}
	
	//getCurrentFloor
	//getId;
	//getElevatorState;
	//get elevatiorDirection
	//+add stop
	//-addStep
	//handleDownstop
	//handleUpStop
	//openDoor
	//closeDoor
	//calculateDistance
	//moveTowards
	
	public int getElevatorId() {
		return elevator_id;
	}
	
	public synchronized int getCurrentFloor() {
		return this.currentFloor;
	}
	
	public synchronized ElevatorState getCurrentState() {
		return this.elevatorState;
	}
	
	public synchronized Direction getElevatorDirection() {
		return this.elevatorDirection;
	}
	
	public synchronized void addStop(int floor) {
		
		if(currentFloor==floor) {
			return;
		}
		if(currentFloor<floor){
			upstops.add(floor);
		}else if(currentFloor>floor) {
			downstops.add(floor);
		}
		
		if(elevatorDirection.equals(elevatorDirection.IDLE)) {
			elevatorDirection=(currentFloor<floor)?elevatorDirection.UP:elevatorDirection.DOWN;
			elevatorState=ElevatorState.MOVING;
			
		}
		System.out.println(
			    "Executing Thread: " + Thread.currentThread().getName()
			    + " | Elevator ID: " + elevator_id
			    + " | Stop added for floor: " + floor
			);	}
	
	public int distanceFrom(int floor) {
		return Math.abs(floor-currentFloor);
	}
	
	public void run() {
		running=true; 
		System.out.println(Thread.currentThread().getName() + " started for Elevator " + elevator_id);
		while(running) {
		step();	
		try {
             Thread.sleep(tickDurationMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
			running=false;
		}
		}
		System.out.println(Thread.currentThread().getName() + " Elevator stopped: " + elevator_id);

	}
	
	public void step(){
		if(doorState==doorState.OPEN) {
			closeDoor();
			return;
		}
		if(elevatorDirection==elevatorDirection.UP) {
			handleUpStops();
		}
		if(elevatorDirection==elevatorDirection.DOWN) {
			handleDownStops();
		}
	}
	
	public void handleUpStops(){
		if(!upstops.isEmpty()) {
			int target=upstops.first();
			if(target==currentFloor) {
				upstops.remove(currentFloor);
				openDoor();
			}else {
				moveTowards(target);
			}
		}else if(!downstops.isEmpty()) {
			elevatorDirection=Direction.DOWN;
		}else {
			elevatorDirection=Direction.IDLE;
			elevatorState=ElevatorState.IDLE;
		}
	}
	
	public void handleDownStops(){
		if(!downstops.isEmpty()) {
			int target=downstops.first();
			if(target==currentFloor) {
				downstops.remove(currentFloor);
				openDoor();
			}else {
				moveTowards(target);
			}
		}else if(!upstops.isEmpty()) {
			elevatorDirection=Direction.UP;
		}else {
			elevatorDirection=Direction.IDLE;
			elevatorState=ElevatorState.IDLE;
		}
	}
	
	public void moveTowards(int floor) {
		elevatorState = ElevatorState.MOVING;
		if(floor<currentFloor) {
			currentFloor--;
		}
		else if(floor>currentFloor) {
			currentFloor++;
		}
	}
	
	public void openDoor(){
			doorState=doorState.OPEN;
			elevatorState = ElevatorState.STOPPED;
	        System.out.println("Elevator " + elevator_id + " -> door OPEN at floor " + currentFloor);

	}
	
	public void closeDoor() {
		doorState=doorState.CLOSED;
        System.out.println("Elevator " + elevator_id + " -> door CLOSED at floor " + currentFloor);

	}
	
	public void shutdown() {
		running=false;
	}
	
	
}


class ElevatorRequest{
	private int floor;
	private Direction direction;
	ElevatorRequest(int floor, Direction direction) {
		this.floor=floor;
		this.direction=direction;
	}
	public int getFloor() {
		return floor;
	}
	public Direction getDirection() {
		return direction;
	}
}

interface ElevatorSelectionStrategy{
	
	public Elevator assignElevator(List<Elevator> elevators, ElevatorRequest request);
}

class NearestElevatorStategy implements ElevatorSelectionStrategy{

	@Override
	public Elevator assignElevator(List<Elevator> elevators, ElevatorRequest request) {
		
		Elevator opElevator=null;
		int minDiff=Integer.MAX_VALUE;
		
		for(Elevator elevator:elevators) {
			if(elevator.getElevatorDirection()==Direction.IDLE) {
			minDiff=Math.min(minDiff, elevator.distanceFrom(request.getFloor()));
			opElevator=elevator;
			}else {
				if(elevator.distanceFrom(request.getFloor())<minDiff) {
					if(elevator.getCurrentFloor()<request.getFloor()&& request.getDirection()==Direction.UP) {
						minDiff=Math.min(minDiff, elevator.distanceFrom(request.getFloor()));
						opElevator=elevator;
					}else if(elevator.getCurrentFloor()>request.getFloor()&& request.getDirection()==Direction.DOWN) {
						minDiff=Math.min(minDiff, elevator.distanceFrom(request.getFloor()));
						opElevator=elevator;
					}
				}
			}
		}
		
		 if (opElevator == null) {
	            for (Elevator elevator : elevators) {
	                int distance = elevator.distanceFrom(request.getFloor());
	                if (distance < minDiff) {
	                	minDiff = distance;
	                	opElevator = elevator;
	                }
	            }
	        }
		
		return opElevator;
	}
	
}

class ElevatorController{
	ElevatorSelectionStrategy strategy;
	List<Elevator> elevators;
	List<Thread> elevatorsThread;
	
	ElevatorController(ElevatorSelectionStrategy strategy, List<Elevator> elevators) {
		this.strategy=strategy;
		this.elevators=elevators;
		elevatorsThread=new ArrayList<>();
	}
	
	public List<Elevator> getElevators() {
		return this.elevators;
	}
	
	public void start() {
		for(Elevator elevator:elevators) {
			Thread thread=new Thread(elevator,"elevator-"+elevator.getElevatorId()+"-thread");
			elevatorsThread.add(thread);
			thread.start();
		}
	}
	
	public void assignElevator(int floor, Direction direction) {
		ElevatorRequest request=new ElevatorRequest(floor,direction);
		 Elevator chosen = strategy.assignElevator(elevators, request);

		    if (chosen != null) {
		        System.out.println(
		            "Request Thread: " + Thread.currentThread().getName()
		            + " | Selected Elevator: " + chosen.getElevatorId()
		            + " | Elevator Thread: Elevator-"
		            + chosen.getElevatorId() + "-Thread"
		            + " | Requested Floor: " + floor
		        );

		        chosen.addStop(floor);
		    } else {
		        System.out.println("No elevator available for floor " + floor);
		    }
		
	}
	
	//call from inside elevator
	public void selectFloor(int elevatorId, int floor) {
		for(Elevator elevator:elevators) {
			if(elevator.getElevatorId()==elevatorId) {
				elevator.addStop(floor);
			}
		}
	}
	
	public void shutdown() throws InterruptedException {
		for(Elevator elevator: elevators) {
			elevator.shutdown();
		}
		for(Thread thread:elevatorsThread) {
			thread.join();
		}
	}
	
}

class Building{
	ElevatorController elevatorController;
	int floor;
	
	Building(ElevatorController elevatorController,int floor){
		this.floor=floor;
		this.elevatorController=elevatorController;
	}
	
}



