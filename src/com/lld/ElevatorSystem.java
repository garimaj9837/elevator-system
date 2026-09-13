package com.lld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

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
 *   
 *   
 *   Classes and Entites
 *   
 *   	Elevator
 *   	  addFloor()
 *   	Request
 *   	Scheduling Strategy
 *   		-> NearestDistanceStrategy
 *   	Direction, Elevator State, Door State enums
 *   
 *   	
 * 
 * 
 */

enum DoorState{
	CLOSED,OPEN
}

enum Direction{
	UP,DOWN,IDLE
}

enum ElevatorState {
    MOVING,
    STOPPED,
    IDLE,
    MAINTENANCE
}

interface Request{
	
}

class HallCallRequest implements Request{
	int sourceFloor;
	Direction direction;
//	--can add
//	priority
//	timestamp
//	requestId
	HallCallRequest(int sourceFloor,Direction direction){
		this.sourceFloor=sourceFloor;
		this.direction=direction;
	}
}

class CabinCallRequest implements Request{
	int destinationFloor;
}

class Elevator{
	int id;
	Direction direction;
	int currentFloor;
	double capacity;
	DoorState doorState;
	PriorityQueue<Integer> upStops=new PriorityQueue<>();
	PriorityQueue<Integer> downStops=new PriorityQueue<>(Collections.reverseOrder());
	
	Elevator(){
		currentFloor=0;
		direction=Direction.IDLE;
		doorState=DoorState.CLOSED;
	}
	public void addRequest(int floor) {
		if(floor>currentFloor) {
			upStops.add(floor);
		}else if(floor<currentFloor) {
			downStops.add(floor);
		}
	}	
	
}

interface SchedulingStrategy{
	public Elevator selectElevator(List<Elevator> elevators,HallCallRequest request) ;
}

class NearestElevatorStrategy implements SchedulingStrategy{

	@Override
	public Elevator selectElevator(List<Elevator> elevators,HallCallRequest request) {
			int min=Integer.MAX_VALUE;
			Elevator outputElevator=null;
			for(Elevator e:elevators) {
				if(Math.abs(e.currentFloor-request.sourceFloor)<min) {
					min=Math.abs(e.currentFloor-request.sourceFloor);
					outputElevator=e;
				}
			}
			return outputElevator;
		
	}
	
}

class ElevatorController{
	List<Elevator> elevators=new ArrayList<>();
	SchedulingStrategy strategy;
	
	ElevatorController(SchedulingStrategy strategy){
		this.strategy=strategy;
	}
	
	public void addElevator(Elevator elevator) {
		elevators.add(elevator);
	}
	
	public void removeElevator(Elevator elevator) {
		elevators.remove(elevator);
	}
	
	public void hallCall(HallCallRequest request) {
		Elevator outputElevator=strategy.selectElevator(elevators, request);
		outputElevator.addRequest(request.sourceFloor);
	}
}

public class ElevatorSystem {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SchedulingStrategy strategy=new NearestElevatorStrategy();
		ElevatorController ec =new ElevatorController(strategy);
		Elevator elevator1=new Elevator();
		ec.addElevator(elevator1);
		
		HallCallRequest request=new HallCallRequest(5,Direction.UP);
		HallCallRequest request2=new HallCallRequest(4,Direction.DOWN);
		ec.hallCall(request);
		ec.hallCall(request2);
		
	}

}
